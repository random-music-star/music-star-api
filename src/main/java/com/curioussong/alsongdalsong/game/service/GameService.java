package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.chat.dto.ChatRequest;
import com.curioussong.alsongdalsong.game.board.BoardEventHandler;
import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.game.domain.InGameManager;
import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.dto.board.BoardEventResponseDTO;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfo;
import com.curioussong.alsongdalsong.game.event.GameStatusEvent;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import com.curioussong.alsongdalsong.game.timer.GameTimerManager;
import com.curioussong.alsongdalsong.game.util.SongAnswerValidator;
import com.curioussong.alsongdalsong.member.domain.Member;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final MemberService memberService;
    private final RoomRepository roomRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final RoomManager roomManager;
    private final InGameManager inGameManager;
    private final GameMessageSender gameMessageSender;
    private final GameTimerManager gameTimerManager;
    private final BoardEventHandler boardEventHandler;

    @Transactional
    public void startGame(Long channelId, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        log.debug("Starting game in room {}", roomId);
        if (!roomManager.areAllPlayersReady(roomId)) {
            log.debug("Not all players are ready in room {}. Cancelling game start.", roomId);
            return;
        }
        log.debug("All players are ready, proceeding to start the game.");
        eventPublisher.publishEvent(new GameStatusEvent(room, "IN_PROGRESS"));

        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);
        gameMessageSender.sendRoomInfoToSubscriber(destination, room, roomManager.getSelectedYears(roomId));

        inGameManager.initializeGameSettings(room);
        startRound(channelId, room, destination);

        // TODO : readyStatusMap.remove(roomId);
    }

    public void startRound(Long channelId, Room room, String destination) {
        Long roomId = room.getId();
        int currentRound = inGameManager.getCurrentRound(roomId);
        log.info("Round : {} 진행중입니다.", currentRound);

        // 목표 지점 도달한 사용자 존재 시 종료
        String winner = findWinnerByScore(roomId, 19);
        if (winner != null) {
            log.info("Player {} reached 20 points, ending game.", winner);
            endGame(roomId, destination);
            return;
        }

        // 최대 라운드 도달 시 종료
        if (currentRound == roomManager.getMaxGameRound(roomId)+1) {
            endGame(roomId, destination);
            return;
        }

        // 다음 라운드 준비
        gameTimerManager.initializeSchedulers(roomId); // 타이머 초기화
        inGameManager.initializeRoundWinner(room); // 라운드 정답자 초기화
        inGameManager.initializeSkipStatus(room); // 스킵 초기화

        // Todo : 하드코딩된 gameMode를 사용자가 설정한 값으로 불러오도록 수정 필요
        gameMessageSender.sendRoundInfo(destination, currentRound, GameMode.FULL);

        sendQuizInfo(destination, roomId);
        startCountdown(destination, channelId, roomId);
    }

    public void startCountdown(String destination, Long channelId, Long roomId) {
        gameTimerManager.startCountdown(destination, roomId, () -> {
            // 카운트다운 완료 후 실행될 코드
            gameTimerManager.scheduleSongPlayTime(
                    destination,
                    inGameManager.getCurrentRoundSong(roomId).getPlayTime(),
                    roomId,
                    () -> triggerEndEvent(destination, channelId, roomId)
            );
            inGameManager.updateIsSongPlaying(roomId);
        });
    }

    private void triggerEndEvent(String destination, Long channelId, Long roomId) {
        log.debug("End event for channel {} in room {}", channelId, roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));

        inGameManager.updateIsSongPlaying(roomId);

        boolean isWinnerExist = (inGameManager.getRoundWinner(roomId).get(roomId) != null);
        String winner = inGameManager.getRoundWinner(roomId).get(roomId);

        gameMessageSender.sendNextMessage(destination,
                winner,
                isWinnerExist ? 4 : 0);

        if (isWinnerExist) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            handleSendingPositionMessage(destination, roomId, winner);
        } else { // 정답자 없이 스킵된 경우 바로 다음 라운드 시작
            sendGameResult(destination, roomId, winner);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        inGameManager.nextRound(roomId);
        inGameManager.updateIsAnswered(roomId);
        startRound(channelId, room, destination);
    }

    private void handleSendingPositionMessage(String destination, Long roomId, String currentRoundWinner) {
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

    private void sendQuizInfo(String destination, Long roomId) {
        Song song = inGameManager.getCurrentRoundSong(roomId);
        gameMessageSender.sendQuizInfo(destination, song.getUrl());
    }

    @Transactional
    public void sendRoomInfoAndUserInfoToSubscriber(Long channelId, Long roomId) {
        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        gameMessageSender.sendRoomInfoToSubscriber(destination, room, roomManager.getSelectedYears(roomId));
        sendUserInfoToSubscriber(destination, room);
    }

    private void sendUserInfoToSubscriber(String destination, Room room) {
        List<Long> memberIds = room.getMembers().stream()
                .map(Member::getId).toList();
        List<UserInfo> userInfoList = new ArrayList<>();

        boolean allReady = true;

        for (Long memberId : memberIds) {
            Member member = memberService.getMemberById(memberId);
            boolean isHost = memberId.equals(room.getHost().getId());
            boolean isReady = Boolean.TRUE.equals(roomManager.getReady(room.getId(), memberId));

            log.debug("User {} ready status in response: {}", member.getUsername(), isReady);

            if (!isReady) {
                allReady = false;
            }

            UserInfo userInfo = new UserInfo(member.getUsername(), isReady, isHost);
            userInfoList.add(userInfo);
        }

        gameMessageSender.sendUserInfo(destination, userInfoList, allReady);
    }

    private void sendGameResult(String destination, Long roomId, String userName) {
        Song song = inGameManager.getCurrentRoundSong(roomId);
        gameMessageSender.sendGameResult(destination, userName, song);
    }

    public boolean checkAnswer(ChatRequest chatRequest, Long roomId) {
        String userAnswer = chatRequest.getRequest().getMessage();
        Song song = inGameManager.getCurrentRoundSong(roomId);
        log.info("current song: {}", song.getKorTitle());
        return SongAnswerValidator.isCorrectAnswer(userAnswer, song.getKorTitle(), song.getEngTitle());
    }

    public void handleAnswer(String userName, Long channelId, Long roomId) {
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

        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);
        gameTimerManager.cancelHintTimers(roomId);

        sendGameResult(destination, roomId, userName);
    }

    @Transactional
    public void incrementSkipCount(Long roomId, Long channelId, String username) {
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
        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);

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
            gameTimerManager.cancelRoundTimerAndTriggerImmediately(roomId, () -> triggerEndEvent(destination, channelId, roomId));
        }
    }

//    Todo: 방에 입장한 유저만 레디 상태 변경이 가능하도록 변경 필요
    @Transactional
    public void toggleReady(String username, Long channelId, Long roomId) {
        Map<Long, Boolean> roomReadyStatus = roomManager.getReadyStatus(roomId);
        Member member = memberService.getMemberByToken(username);
        boolean currentReady = Boolean.TRUE.equals(roomReadyStatus.getOrDefault(member.getId(), false));

        boolean newReady = !currentReady;
        log.debug("User {} ready status: {} -> {}", username, currentReady, newReady);

        Long memberId = memberService.getMemberByToken(username).getId();
        roomReadyStatus.put(memberId, !currentReady);

        log.debug("Room {} ready status map: {}", roomId, roomReadyStatus);

        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        sendUserInfoToSubscriber(destination, room);
    }

    public void updateSongYears(Long roomId, List<Integer> selectedYears) {
        roomManager.setSelectedYears(roomId, selectedYears);
    }

    // 최고 점수를 가진 플레이어 찾기
    private String findWinnerByScore(Long roomId, int minScore) {
        return inGameManager.getScore(roomId).entrySet()
                .stream()
                .filter(entry -> entry.getValue() >= minScore)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private void endGame(Long roomId, String destination) {
        // 모든 타이머 종료
        gameTimerManager.shutdownAllTimers(roomId);

        // 최고 점수 가진 플레이어 찾기
        String finalWinner = findWinnerByScore(roomId, 0);
        log.info("Game ended. Final winner: {}", finalWinner);
        gameMessageSender.sendGameEndMessage(destination, finalWinner);

        // 인게임 정보 삭제
        inGameManager.clear(roomId);

        // 멤버 상태 초기화
        roomManager.initializeMemberReadyStatus(roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));

        eventPublisher.publishEvent(new GameStatusEvent(
                roomRepository.findById(roomId)
                        .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다.")),
                "WAITING"));
        ScheduledExecutorService tempScheduler = Executors.newSingleThreadScheduledExecutor();
        tempScheduler.schedule(() -> {
            gameMessageSender.sendRoomInfoToSubscriber(destination, room, roomManager.getSelectedYears(roomId));
            sendUserInfoToSubscriber(destination, room);
            tempScheduler.shutdown();
        }, 3, TimeUnit.SECONDS);
    }
}