package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.common.error.stomperror.StompError;
import com.curioussong.alsongdalsong.common.error.stomperror.StompException;
import com.curioussong.alsongdalsong.common.util.Destination;
import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.game.domain.InGameManager;
import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.dto.chat.ChatRequestDTO;
import com.curioussong.alsongdalsong.game.dto.song.SongInfo;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfo;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import com.curioussong.alsongdalsong.game.timer.GameTimerManager;
import com.curioussong.alsongdalsong.game.util.SongAnswerValidator;
import com.curioussong.alsongdalsong.member.service.MemberService;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractGameStrategy implements GameStrategy {

    protected final InGameManager inGameManager;
    protected final GameTimerManager gameTimerManager;
    protected final GameMessageSender gameMessageSender;
    protected final RoomManager roomManager;

    private final MemberService memberService;
    private final RoomRepository roomRepository;

    @Override
    public boolean checkAnswer(ChatRequestDTO dto, String roomId) {
        // 노래가 재생 중이지 않다면 정답 처리 하지 않음.
        if (!inGameManager.getIsSongPlaying(roomId)) {
            return false;
        }

        String userAnswer = dto.getRequest().getMessage();
        GameMode gameMode = inGameManager.getCurrentRoundGameMode(roomId);

        SongInfo firstSong = inGameManager.getCurrentRoundSong(roomId);
        SongInfo secondSong = inGameManager.hasSecondSongInCurrentRound(roomId)
                ? inGameManager.getSecondSongForCurrentRound(roomId) : null;

        boolean isCorrect = SongAnswerValidator.isCorrectAnswer(
                userAnswer,
                gameMode,
                firstSong.getKorTitle(),
                firstSong.getEngTitle(),
                secondSong != null ? secondSong.getKorTitle() : "",
                secondSong != null ? secondSong.getEngTitle() : ""
        );

        if (isCorrect) {
            inGameManager.setSubmittedAnswer(roomId, userAnswer);
        }
        return isCorrect;
    }

    @Override
    public void initializeRound(Room room) {
        gameTimerManager.initializeSchedulers(room.getId()); // 타이머 초기화
        inGameManager.initializeRoundWinner(room); // 라운드 정답자 초기화
        inGameManager.initializeSkipStatus(room); // 스킵 초기화
    }

    @Override
    @Transactional
    public void incrementSkipCount(String roomId, Long channelId, String username) {
        Long memberId = memberService.getMemberByToken(username).getId();

        // 이미 스킵을 한 사용자라면
        if (inGameManager.isSkipped(roomId, memberId)) {
            return;
        }

        // 노래가 재생 중일 때만 스킵 가능
        if (!inGameManager.getIsSongPlaying(roomId)) {
            return;
        }

        // 스킵 상태 변환
        inGameManager.setSkip(roomId, memberId);
        int currentSkipCount = inGameManager.getSkipCount(roomId);
        String destination = Destination.room(channelId, roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new StompException(StompError.ROOM_NOT_FOUND));
        int participantCount = room.getMembers().size();

        log.debug("Room {} skip count: {}/{}", roomId, currentSkipCount, participantCount);

        // 스킵 정보 전송
        gameMessageSender.sendSkipInfo(destination, currentSkipCount);

        // 참가자 절반 초과 시 즉시 다음 라운드 시작
        if (currentSkipCount > participantCount / 2) {
            log.info("Skip count exceeded threshold, moving to next round.");
            gameTimerManager.cancelHintTimers(roomId);
            gameTimerManager.cancelRoundTimerAndTriggerImmediately(roomId, () -> triggerEndEvent(destination, channelId, room));
        }
    }

    @Override
    public List<String> findWinnerByScore(String roomId) {
        Map<String, Integer> scores = inGameManager.getScore(roomId);
        List<String> winners = new ArrayList<>();

        int maxScore = Integer.MIN_VALUE;
        for (Integer score : scores.values()) {
            if (score > maxScore) {
                maxScore = score;
            }
        }

        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getValue() == maxScore) {
                winners.add(entry.getKey());
            }
        }

        return winners;
    }

    @Override
    public void sendGameResult(String destination, String roomId, String userName) {
        SongInfo firstSong = inGameManager.getCurrentRoundSong(roomId);
        SongInfo secondSong = null;
        if (inGameManager.hasSecondSongInCurrentRound(roomId)) {
            secondSong = inGameManager.getSecondSongForCurrentRound(roomId);
        }
        gameMessageSender.sendGameResult(destination, userName, firstSong, secondSong);
    }

    @Override
    public void handleAnswer(String userName, Long channelId, String roomId) {
        // 이미 정답을 맞춘 라운드이면 통과
        if (inGameManager.isAnswered(roomId)) {
            return;
        }

        // 맵에 따라 다르게 동작
        addScore(userName, roomId);

        // 방의 현재 라운드 정답자 저장
        inGameManager.getRoundWinner(roomId).put(roomId, userName);

        // 정답 맞춘 상태 처리
        inGameManager.markAsAnswered(roomId);

        String destination = Destination.room(channelId, roomId);
        gameTimerManager.cancelHintTimers(roomId);

        sendGameResult(destination, roomId, userName);
    }

    @Override
    public void startRound(Long channelId, Room room, String destination) {
        int currentRound = inGameManager.getCurrentRound(room.getId());
        log.info("Round : {} 진행중입니다.", currentRound);

        // 마지막 라운드 종료 시 게임 종료
        if (isGameEnd(room, currentRound)) {
            endGame(room, destination);
            return;
        }

        // 라운드 준비
        initializeRound(room);
        handleRoundStart(destination, channelId, room);
    }

    @Override
    public void endGame(Room room, String destination) {
        if (room.getMembers().isEmpty() || inGameManager.getInGameInfo(room.getId()) == null) {
            log.info("방 {} 게임 종료 - 멤버가 없거나 게임 정보가 삭제되어 처리를 중단합니다.", room.getId());
            return;
        }
        // 모든 타이머 종료
        gameTimerManager.shutdownAllTimers(room.getId());

        // 최고 점수 가진 플레이어 찾기
        List<String> finalWinner = findWinnerByScore(room.getId());
        log.info("Game ended. Final winner: {}", finalWinner);
        gameMessageSender.sendGameEndMessage(destination, finalWinner);

        // 멤버 상태 초기화
        roomManager.initializeMemberReadyStatus(room.getId());

        // 게임 종료 시 WAITING 상태로 변경
        room.updateStatus(Room.RoomStatus.WAITING);

        publishGameEndEvents(room);

        ScheduledExecutorService tempScheduler = Executors.newSingleThreadScheduledExecutor();
        tempScheduler.schedule(() -> {
            gameMessageSender.sendRoomInfo(destination, room, roomManager.getSelectedYears(room.getId()), roomManager.getGameModes(room.getId()));

            List<UserInfo> userInfoList = roomManager.getUserInfos(room);
            boolean allReady = roomManager.isAllReady(room);
            gameMessageSender.sendUserInfo(destination, userInfoList, allReady);

            tempScheduler.shutdown();

            // 인게임 정보 삭제
            inGameManager.clear(room.getId());
        }, 3, TimeUnit.SECONDS);
    }

    protected abstract void addScore(String userName, String roomId);
    protected abstract boolean isGameEnd(Room room, int currentRound);
    protected abstract void publishGameEndEvents(Room room);
}
