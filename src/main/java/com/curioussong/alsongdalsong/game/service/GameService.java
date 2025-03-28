package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.game.board.BoardEventHandler;
import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.game.domain.InGameManager;
import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.dto.board.BoardEventResponseDTO;
import com.curioussong.alsongdalsong.game.dto.chat.ChatRequestDTO;
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

    public void channelChatMessage(ChatRequestDTO chatRequestDTO, Long channelId) {
        String destination = String.format("/topic/channel/%d", channelId);

        gameMessageSender.sendChat(chatRequestDTO, destination);
    }

    public void roomChatMessage(ChatRequestDTO chatRequestDTO, Long channelId, String roomId) {
        String destination = String.format("/topic/channel/%d/room/%s", channelId, roomId);
        gameMessageSender.sendChat(chatRequestDTO, destination);

        // Skip 요청 처리
        if (".".equals(chatRequestDTO.getRequest().getMessage())) {
            incrementSkipCount(roomId, channelId, chatRequestDTO.getRequest().getSender());
        }

        if (checkAnswer(chatRequestDTO, roomId)) {
            handleAnswer(chatRequestDTO.getRequest().getSender(), channelId, roomId);
        }

    }



    @Transactional
    public void startGame(Long channelId, String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        log.debug("Starting game in room {}", roomId);
        if (!roomManager.areAllPlayersReady(roomId)) {
            log.debug("Not all players are ready in room {}. Cancelling game start.", roomId);
            return;
        }
        log.debug("All players are ready, proceeding to start the game.");
        eventPublisher.publishEvent(new GameStatusEvent(room, "IN_PROGRESS"));

        String destination = String.format("/topic/channel/%d/room/%s", channelId, roomId);
        gameMessageSender.sendRoomInfoToSubscriber(destination, room, roomManager.getSelectedYears(roomId));

        inGameManager.initializeGameSettings(room);
        startRound(channelId, room, destination);

        // TODO : readyStatusMap.remove(roomId);
    }

    public void startRound(Long channelId, Room room, String destination) {
        int currentRound = inGameManager.getCurrentRound(room.getId());
        Song currentRoundSong = inGameManager.getCurrentRoundSong(room.getId());
        log.info("Round : {} 진행중입니다.", currentRound);

        // 사용자가 목표 지점에 도달 || 최대 라운드 도달 시 종료
        if (isGameEnd(room, currentRound)) {
            endGame(room, destination);
        }

        // 라운드 준비
        initializeRound(room);

        gameMessageSender.sendRoundInfoAndQuizInfo(destination, currentRound, GameMode.FULL, currentRoundSong);

        startCountdown(destination, channelId, room);
    }

    private boolean isGameEnd(Room room, int currentRound) {
        return findWinnerByScore(room.getId(), 19) != null || currentRound == roomManager.getMaxGameRound(room.getId()) + 1;
    }

    private void initializeRound(Room room) {
        gameTimerManager.initializeSchedulers(room.getId()); // 타이머 초기화
        inGameManager.initializeRoundWinner(room); // 라운드 정답자 초기화
        inGameManager.initializeSkipStatus(room); // 스킵 초기화
    }

    public void startCountdown(String destination, Long channelId, Room room) {
        gameTimerManager.startCountdown(destination, room.getId(), () -> {
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

    @Transactional
    public void sendRoomInfoAndUserInfoToSubscriber(Long channelId, String roomId) {
        String destination = String.format("/topic/channel/%d/room/%s", channelId, roomId);
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

    private void sendGameResult(String destination, String roomId, String userName) {
        Song song = inGameManager.getCurrentRoundSong(roomId);
        gameMessageSender.sendGameResult(destination, userName, song);
    }

    public boolean checkAnswer(ChatRequestDTO chatRequestDTO, String roomId) {
        String userAnswer = chatRequestDTO.getRequest().getMessage();
        Song song = inGameManager.getCurrentRoundSong(roomId);
        log.info("current song: {}", song.getKorTitle());
        return SongAnswerValidator.isCorrectAnswer(userAnswer, song.getKorTitle(), song.getEngTitle());
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

//    Todo: 방에 입장한 유저만 레디 상태 변경이 가능하도록 변경 필요
    @Transactional
    public void toggleReady(String username, Long channelId, String roomId) {
        Map<Long, Boolean> roomReadyStatus = roomManager.getReadyStatus(roomId);
        Member member = memberService.getMemberByToken(username);
        boolean currentReady = Boolean.TRUE.equals(roomReadyStatus.getOrDefault(member.getId(), false));

        boolean newReady = !currentReady;
        log.debug("User {} ready status: {} -> {}", username, currentReady, newReady);

        Long memberId = memberService.getMemberByToken(username).getId();
        roomReadyStatus.put(memberId, !currentReady);

        log.debug("Room {} ready status map: {}", roomId, roomReadyStatus);

        String destination = String.format("/topic/channel/%d/room/%s", channelId, roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        sendUserInfoToSubscriber(destination, room);
    }

    public void updateSongYears(String roomId, List<Integer> selectedYears) {
        roomManager.setSelectedYears(roomId, selectedYears);
    }

    // 최고 점수를 가진 플레이어 찾기
    private String findWinnerByScore(String roomId, int minScore) {
        return inGameManager.getScore(roomId).entrySet()
                .stream()
                .filter(entry -> entry.getValue() >= minScore)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private void endGame(Room room, String destination) {
        // 모든 타이머 종료
        gameTimerManager.shutdownAllTimers(room.getId());

        // 최고 점수 가진 플레이어 찾기
        String finalWinner = findWinnerByScore(room.getId(), 0);
        log.info("Game ended. Final winner: {}", finalWinner);
        gameMessageSender.sendGameEndMessage(destination, finalWinner);

        // 인게임 정보 삭제
        inGameManager.clear(room.getId());

        // 멤버 상태 초기화
        roomManager.initializeMemberReadyStatus(room.getId());

        eventPublisher.publishEvent(new GameStatusEvent(
                roomRepository.findById(room.getId())
                        .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다.")),
                "WAITING"));
        ScheduledExecutorService tempScheduler = Executors.newSingleThreadScheduledExecutor();
        tempScheduler.schedule(() -> {
            gameMessageSender.sendRoomInfoToSubscriber(destination, room, roomManager.getSelectedYears(room.getId()));
            sendUserInfoToSubscriber(destination, room);
            tempScheduler.shutdown();
        }, 3, TimeUnit.SECONDS);
    }
}