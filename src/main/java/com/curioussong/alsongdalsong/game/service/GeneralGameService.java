package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.game.domain.InGameManager;
import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.dto.chat.ChatRequestDTO;
import com.curioussong.alsongdalsong.game.dto.song.SongInfo;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfo;
import com.curioussong.alsongdalsong.game.event.GameStatusEvent;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import com.curioussong.alsongdalsong.game.timer.GameTimerManager;
import com.curioussong.alsongdalsong.game.util.SongAnswerValidator;
import com.curioussong.alsongdalsong.gameround.event.GameRoundLogEvent;
import com.curioussong.alsongdalsong.gamesession.event.GameSessionLogEvent;
import com.curioussong.alsongdalsong.member.service.MemberService;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import com.curioussong.alsongdalsong.song.domain.Song;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeneralGameService {

    private final MemberService memberService;
    private final RoomRepository roomRepository;

    private final RoomManager roomManager;
    private final InGameManager inGameManager;
    private final GameMessageSender gameMessageSender;
    private final GameTimerManager gameTimerManager;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void startRound(Long channelId, Room room, String destination) {
        int currentRound = inGameManager.getCurrentRound(room.getId());
        log.info("Round : {} 진행중입니다.", currentRound);

        // 마지막 라운드 종료 시 게임 종료
        if (currentRound == roomManager.getMaxGameRound(room.getId())+1) {
            endGame(room, destination);
            return;
        }

        // 라운드 준비
        initializeRound(room);
        handleRoundStart(destination, channelId, room);
    }

    private void initializeRound(Room room) {
        gameTimerManager.initializeSchedulers(room.getId()); // 타이머 초기화
        inGameManager.initializeRoundWinner(room); // 라운드 정답자 초기화
        inGameManager.initializeSkipStatus(room); // 스킵 초기화
    }

    public void handleRoundStart(String destination, Long channelId, Room room) {
        int currentRound = inGameManager.getCurrentRound(room.getId());
        GameMode gameMode = inGameManager.getCurrentRoundGameMode(room.getId());
        SongInfo currentRoundSong = inGameManager.getCurrentRoundSong(room.getId());

        int songPlayTime = currentRoundSong.getPlayTime();

        SongInfo secondSong = null;
        if (inGameManager.hasSecondSongInCurrentRound(room.getId())) {
            secondSong = inGameManager.getSecondSongForCurrentRound(room.getId());
            songPlayTime = Math.min(songPlayTime, secondSong.getPlayTime());
        }

        applicationEventPublisher.publishEvent(new GameRoundLogEvent(
                room,
                GameRoundLogEvent.Type.START,
                currentRound,
                gameMode,
                currentRoundSong,
                secondSong,
                LocalDateTime.now(),
                null,
                null
        ));

        int finalSongPlayTime = songPlayTime;
        gameTimerManager.handleRoundStart(destination, currentRound, gameMode, currentRoundSong, secondSong, room.getId(), () -> {
            // 카운트다운 완료 후 실행될 코드
            inGameManager.resetAnswered(room.getId());

            gameTimerManager.scheduleSongPlayTime(
                    destination,
                    finalSongPlayTime,
                    room.getId(),
                    () -> triggerEndEvent(destination, channelId, room)
            );
            inGameManager.updateIsSongPlaying(room.getId());
        });
    }

    private void triggerEndEvent(String destination, Long channelId, Room room) {
        log.debug("End event for channel {} in room {}", channelId, room.getId());

        inGameManager.updateIsSongPlaying(room.getId());

        boolean isWinnerExist = (inGameManager.getRoundWinner(room.getId()).get(room.getId()) != null);
        String winner = inGameManager.getRoundWinner(room.getId()).get(room.getId());

        if (isWinnerExist) {
            int winnerScore = inGameManager.getScore(room.getId()).get(winner);
            gameMessageSender.sendUserPosition(destination, winner, winnerScore);
        } else {
            sendGameResult(destination, room.getId(), null);
        }

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        applicationEventPublisher.publishEvent(new GameRoundLogEvent(
                room,
                GameRoundLogEvent.Type.END,
                inGameManager.getCurrentRound(room.getId()),
                null,
                null,
                null,
                LocalDateTime.now(),
                isWinnerExist ? winner : null,
                inGameManager.getSubmittedAnswer(room.getId())
        ));
        inGameManager.clearSubmittedAnswer(room.getId());
        inGameManager.nextRound(room.getId());
        startRound(channelId, room, destination);
    }

    private void endGame(Room room, String destination) {
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
        applicationEventPublisher.publishEvent(new GameStatusEvent(room, "WAITING"));
        applicationEventPublisher.publishEvent(new GameSessionLogEvent(room, GameSessionLogEvent.Type.END));
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

    // 최고 점수를 가진 플레이어 찾기
    private List<String> findWinnerByScore(String roomId) {
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

    @Transactional
    public void incrementSkipCount(String roomId, Long channelId, String username) {
        log.info("try skip");
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
        String destination = String.format("/topic/channel/%d/room/%s", channelId, roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
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

    public boolean checkAnswer(ChatRequestDTO chatRequestDTO, String roomId) {
        if (!inGameManager.getIsSongPlaying(roomId)) {
            return false;
        }
        String userAnswer = chatRequestDTO.getRequest().getMessage();
        GameMode gameMode = inGameManager.getCurrentRoundGameMode(roomId);

        SongInfo firstSong = inGameManager.getCurrentRoundSong(roomId);
        SongInfo secondSong = inGameManager.hasSecondSongInCurrentRound(roomId)
                ? inGameManager.getSecondSongForCurrentRound(roomId)
                : null;

        log.info("current song: {}, second song: {}", firstSong.getKorTitle(), secondSong!=null?secondSong.getKorTitle():"");
        boolean isCorrect = SongAnswerValidator.isCorrectAnswer(
                userAnswer,
                gameMode,
                firstSong.getKorTitle(),
                firstSong.getEngTitle(),
                secondSong != null ? secondSong.getKorTitle() : "",
                secondSong != null ? secondSong.getEngTitle() : ""
        );

        if(isCorrect) {
            inGameManager.setSubmittedAnswer(roomId, userAnswer);
        }

        return isCorrect;
    }

    public void handleAnswer(String userName, Long channelId, String roomId) {
        // 이미 정답을 맞춘 라운드이면 통과
        if (inGameManager.isAnswered(roomId)) {
            return;
        }

//        inGameManager.getUserMovement(roomId).put(userName, scoreToAdd);

        // 방의 현재 라운드 정답자 저장
        inGameManager.getRoundWinner(roomId).put(roomId, userName);
        int scoreToAdd = 1;
        inGameManager.getScore(roomId).put(userName, inGameManager.getScore(roomId).get(userName) + scoreToAdd);

        // 정답 맞춘 상태 처리
        inGameManager.markAsAnswered(roomId);

        String destination = String.format("/topic/channel/%d/room/%s", channelId, roomId);
        gameTimerManager.cancelHintTimers(roomId);

        sendGameResult(destination, roomId, userName);
    }

    private void sendGameResult(String destination, String roomId, String userName) {
        SongInfo firstSong = inGameManager.getCurrentRoundSong(roomId);
        SongInfo secondSong = null;
        if (inGameManager.hasSecondSongInCurrentRound(roomId)) {
            secondSong = inGameManager.getSecondSongForCurrentRound(roomId);
        }
        gameMessageSender.sendGameResult(destination, userName, firstSong, secondSong);
    }
}
