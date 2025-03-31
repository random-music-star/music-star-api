package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.game.board.BoardEventHandler;
import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.game.domain.InGameManager;
import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.dto.board.BoardEventResponseDTO;
import com.curioussong.alsongdalsong.game.dto.chat.ChatRequestDTO;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfo;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import com.curioussong.alsongdalsong.game.timer.GameTimerManager;
import com.curioussong.alsongdalsong.game.util.SongAnswerValidator;
import com.curioussong.alsongdalsong.member.service.MemberService;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import com.curioussong.alsongdalsong.song.domain.Song;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardGameService {

    private final MemberService memberService;
    private final RoomRepository roomRepository;

    private final RoomManager roomManager;
    private final InGameManager inGameManager;
    private final GameMessageSender gameMessageSender;
    private final GameTimerManager gameTimerManager;
    private final BoardEventHandler boardEventHandler;

    public void startRound(Long channelId, Room room, String destination) {
        int currentRound = inGameManager.getCurrentRound(room.getId());
        // 사용자가 목표 지점에 도달 || 최대 라운드 도달 시 종료
        if (isGameEnd(room, currentRound)) {
            endGame(room, destination);
            return;
        }
        log.info("Round : {} 진행중입니다.", currentRound);


        // 라운드 준비
        initializeRound(room);
        handleRoundStart(destination, channelId, room);
    }

    private boolean isGameEnd(Room room, int currentRound) {
        return currentRound == roomManager.getMaxGameRound(room.getId()) + 1;
    }

    private void initializeRound(Room room) {
        gameTimerManager.initializeSchedulers(room.getId()); // 타이머 초기화
        inGameManager.initializeRoundWinner(room); // 라운드 정답자 초기화
        inGameManager.initializeSkipStatus(room); // 스킵 초기화
    }

    public void handleRoundStart(String destination, Long channelId, Room room) {
        int currentRound = inGameManager.getCurrentRound(room.getId());
        GameMode gameMode = inGameManager.getRoundInfo(room.getId()).get(currentRound).getFirst();
        Song currentRoundSong = inGameManager.getRoundInfo(room.getId()).get(currentRound).getSecond();
        gameTimerManager.handleRoundStart(destination, currentRound, gameMode, currentRoundSong, room.getId(), () -> {
            // 카운트다운 완료 후 실행될 코드
            gameTimerManager.scheduleSongPlayTime(
                    destination,
                    inGameManager.getCurrentRoundSong(room.getId()).getPlayTime(),
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

        gameMessageSender.sendNextMessage(destination,
                winner,
                isWinnerExist ? 4 : 0);

        if (isWinnerExist) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            handleSendingPositionMessage(destination, room.getId(), winner);
        } else { // 정답자 없이 스킵된 경우 바로 다음 라운드 시작
            sendGameResult(destination, room.getId(), winner);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        inGameManager.nextRound(room.getId());
        inGameManager.updateIsAnswered(room.getId());
        startRound(channelId, room, destination);
    }

    private void handleSendingPositionMessage(String destination, String roomId, String currentRoundWinner) {
        Map<String, Integer> userMovement = inGameManager.getUserMovement(roomId);
        Map<String, Integer> userScores = inGameManager.getScore(roomId);
        while (userMovement.get(currentRoundWinner) > 0) {
            userMovement.put(currentRoundWinner, userMovement.get(currentRoundWinner) - 1);
            userScores.put(currentRoundWinner, userScores.get(currentRoundWinner) + 1);
            gameMessageSender.sendUserPosition(destination, currentRoundWinner, userScores.get(currentRoundWinner));

            if (userScores.get(currentRoundWinner) >= 19) { // 목표 지점 도달 시
                return;
            }

            try {
                Thread.sleep(700);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        int playerCount = userScores.size();

        // 이벤트 생성 및 처리
        BoardEventResponseDTO eventResponse = boardEventHandler.generateEvent(currentRoundWinner, playerCount, roomId);
        boardEventHandler.handleEvent(destination, roomId, eventResponse);
    }

    private void sendGameResult(String destination, String roomId, String userName) {
        Song song = inGameManager.getCurrentRoundSong(roomId);
        gameMessageSender.sendGameResult(destination, userName, song);
    }

    public boolean checkAnswer(ChatRequestDTO chatRequestDTO, String roomId) {
        String userAnswer = chatRequestDTO.getRequest().getMessage();
        Song song = inGameManager.getCurrentRoundSong(roomId);
        log.info("current song: {}", song.getKorTitle());
        SongAnswerValidator validator = new SongAnswerValidator();
        return validator.isCorrectAnswer(userAnswer, song.getKorTitle(), song.getEngTitle());
    }

    public void handleAnswer(String userName, Long channelId, String roomId) {
        // 이미 정답을 맞춘 라운드이면 통과
        if (inGameManager.isAnswered(roomId)) {
            return;
        }

//      int scoreToAdd = calculateScore(); // 추후 구현
        int scoreToAdd = 4;
        inGameManager.getUserMovement(roomId).put(userName, scoreToAdd); // 정답자 이동 예정 횟수 갱신

        // 방의 현재 라운드 정답자 저장
        inGameManager.getRoundWinner(roomId).put(roomId, userName);

        // 정답 맞춘 상태 처리
        inGameManager.updateIsAnswered(roomId);

        String destination = String.format("/topic/channel/%d/room/%s", channelId, roomId);
        gameTimerManager.cancelHintTimers(roomId);

        sendGameResult(destination, roomId, userName);
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

    // 최고 점수를 가진 플레이어 찾기
    private List<String> findWinnerByScore(String roomId) {
        Map<String, Integer> scores = inGameManager.getScore(roomId);

        // 최고 점수 찾기
        int maxScore = scores.values().stream()
                .max(Integer::compareTo)
                .orElse(Integer.MIN_VALUE);

        // 최고 점수를 가진 모든 플레이어 찾기
        return scores.entrySet().stream()
                .filter(entry -> entry.getValue() == maxScore)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
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
}
