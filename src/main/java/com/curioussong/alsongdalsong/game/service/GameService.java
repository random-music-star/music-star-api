package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.chat.dto.ChatRequest;
import com.curioussong.alsongdalsong.game.board.BoardEventHandler;
import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.dto.board.BoardEventResponseDTO;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfo;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import com.curioussong.alsongdalsong.game.timer.GameTimerManager;
import com.curioussong.alsongdalsong.game.util.SongAnswerValidator;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.service.MemberService;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.game.event.GameStatusEvent;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import com.curioussong.alsongdalsong.song.domain.Song;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final MemberService memberService;
    private final RoomRepository roomRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final RoomManager roomManager;
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

        roomManager.initializeGameSetting(roomId);
        startRound(channelId, roomId);

        // TODO : readyStatusMap.remove(roomId);
    }

    public void startRound(Long channelId, Long roomId) {
        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);

        log.info("Round : {} 진행중입니다.", roomManager.getCurrentRound(roomId));

        // 최대 라운드 도달 시 종료
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        int nowRound = roomManager.getCurrentRound(roomId);
        log.debug("nowRound in startRound Method : {}", nowRound);

        String winner = findWinnerByScore(roomId, 20);

        if(winner != null){
            log.info("Player {} reached 20 points, ending game.", winner);
            endGame(roomId, destination);
            return;
        }
        // 전체 게임 종료 처리
        if (nowRound == room.getMaxGameRound()+1) {
            endGame(roomId, destination);
            return;
        }

        gameTimerManager.initializeSchedulers(roomId);

        roomManager.getRoomInfo(roomId).getRoundWinner().put(roomId, ""); // 라운드 시작 시 현재 라운드 정답자 초기화

        log.info("skip 상태 초기화 시작");

        if (room.getMembers() == null) {
            log.info("room.getMembers() is null!");
        } else {
            log.info("Member IDs: {}", room.getMembers());
            System.out.flush(); // 로그 강제 출력
        }

        // skip 상태 초기화
        roomManager.initializeSkipStatus(roomId);

        log.info("{} 번째 라운드 정보 전송", roomManager.getCurrentRound(roomId));

        log.debug("sendRoundInfo 호출됨 - destination: {}, roomId: {}", destination, roomId);
        // Todo : 하드코딩된 gameMode를 사용자가 설정한 값으로 불러오도록 수정 필요
        gameMessageSender.sendRoundInfo(destination, roomManager.getCurrentRound(roomId), GameMode.FULL);

        log.debug("sendRoundInfo 메시지 전송 완료 - round: {}", roomManager.getCurrentRound(roomId));
        log.info("카운트 다운 시작");
        startCountdown(destination, roomId);
    }

    public void startCountdown(String destination, Long roomId) {
        gameTimerManager.startCountdown(destination, roomId, () -> {
            // 카운트다운 완료 후 실행될 코드
            gameTimerManager.scheduleSongPlayTime(
                    destination,
                    roomManager.getSong(roomId).getPlayTime(),
                    roomId,
                    () -> triggerEndEvent(destination)
            );
            roomManager.initAnswer(roomId);
            sendQuizInfo(destination, roomId);
        });
    }

    private void triggerEndEvent(String destination) {
        log.info("EndEvent 트리거 활성화");

        Pattern pattern = Pattern.compile("^/topic/channel/(\\d+)/room/(\\d+)$");
        Matcher matcher = pattern.matcher(destination);

        if (matcher.matches()) {
            Long channelId = Long.parseLong(matcher.group(1)); // 첫 번째 캡처 그룹 -> channelId
            Long roomId = Long.parseLong(matcher.group(2)); // 두 번째 캡처 그룹 -> roomId

            log.info("channelId: {}, roomdId : {}", destination, roomId);

            roomManager.updateIsSongPlaying(roomId);

            boolean isWinnerExist = !roomManager.getRoomInfo(roomId).getRoundWinner().get(roomId).isEmpty();

            gameMessageSender.sendNextMessage(destination,
                    roomManager.getRoomInfo(roomId).getRoundWinner().get(roomId),
                    isWinnerExist ? 4 : 0);

            if (isWinnerExist) {
                handleSendingPositionMessage(destination, roomId);
            } else { // 정답자 없이 스킵된 경우 바로 다음 라운드 시작
                sendGameResult(destination, roomId, roomManager.getRoomInfo(roomId).getRoundWinner().get(roomId));
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            roomManager.nextRound(roomId);
            startRound(channelId, roomId);
        } else {
            log.info("Pattern did not match destination: {}", destination);
        }
    }

    private void handleSendingPositionMessage(String destination, Long roomId) {
        Map<String, Integer> userMovement = roomManager.getRoomInfo(roomId).getUserMovement();
        Stack<String> userTurn = new Stack<>();
        String firstMover = roomManager.getRoomInfo(roomId).getRoundWinner().get(roomId);
        userTurn.push(firstMover);

        int positionBeforeMove = userMovement.get(firstMover);

        while (!userTurn.empty()) {
            String mover = userTurn.pop();
            while (userMovement.get(mover) > 0) {
                String userWhoMove = roomManager.getRoomInfo(roomId).getRoundWinner().get(roomId);
                userMovement.put(mover, userMovement.get(mover) - 1);
                // 말이 이동하는 사람들의 현재 위치(점수) 갱신
                Map<String, Integer> scoreMap = roomManager.getRoomInfo(roomId).getScore();
                scoreMap.compute(mover, (key, value) -> (value == null) ? 1 : value + 1); // 현재는 앞으로만 가고 다른 이벤트 없으므로 +1

                gameMessageSender.sendUserPosition(destination, userWhoMove, scoreMap.get(userWhoMove));
                if (roomManager.getRoomInfo(roomId).getScore().get(firstMover) >= 20) {
                    return;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }


            }
        }
        // 이벤트 생성 및 처리
        BoardEventResponseDTO eventResponse = boardEventHandler.generateEvent(firstMover);
        boardEventHandler.handleEvent(destination, roomId, eventResponse);
    }

    private void sendQuizInfo(String destination, Long roomId) {
        log.info("퀴즈 정보 전송");
        gameMessageSender.sendQuizInfo(destination, roomManager.getSong(roomId).getUrl());
        roomManager.updateIsSongPlaying(roomId);
        log.info("updateIsSongPlaying : {}", roomManager.getIsSongPlaying(roomId));
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
        Song song = roomManager.getSong(roomId);
        gameMessageSender.sendGameResult(destination, userName, song);
    }

    public boolean checkAnswer(ChatRequest chatRequest, Long roomId) {
        String userAnswer = chatRequest.getRequest().getMessage();
        Song song = roomManager.getSong(roomId);
        return SongAnswerValidator.isCorrectAnswer(userAnswer, song.getKorTitle(), song.getEngTitle());
    }

    public void handleAnswer(String userName, Long channelId, Long roomId) {
        // 이미 정답을 맞춘 라운드이면 통과
        if (roomManager.isAnswered(roomId)) {
            return;
        }

//      int scoreToAdd = calculateScore(); // 추후 구현
        int scoreToAdd = 4;
        roomManager.getRoomInfo(roomId).getUserMovement().put(userName, scoreToAdd); // 정답자 이동 예정 횟수 갱신

        // 방의 현재 라운드 정답자 저장
        Map<Long, String> roundWinner = roomManager.getRoomInfo(roomId).getRoundWinner();
        roundWinner.put(roomId, userName);

        roomManager.setAnswer(roomId);
        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);
        gameTimerManager.cancelHintTimers(roomId);

        sendGameResult(destination, roomId, userName);
    }

    @Transactional
    public void incrementSkipCount(Long roomId, Long channelId, String username) {
        Long memberId = memberService.getMemberByToken(username).getId();

        // 이미 스킵을 한 사용자라면
        if (roomManager.isSkipped(roomId, memberId)) {
            return;
        }

        // 노래가 재생 중일 때만 스킵 가능
        if (!roomManager.getIsSongPlaying(roomId)) {
            return;
        }

        // 스킵 상태 변환
        roomManager.setSkip(roomId, memberId);
        int currentSkipCount = roomManager.raiseSkip(roomId);
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
            gameTimerManager.cancelRoundTimerAndTriggerImmediately(roomId, () -> triggerEndEvent(destination));
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
//        roomAndSelectedYears.put(roomId, selectedYears);
    }

    // 최고 점수를 가진 플레이어 찾기
    private String findWinnerByScore(Long roomId, int minScore) {
        return roomManager.getRoomInfo(roomId).getScore().entrySet()
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

        // 멤버 상태 초기화
        roomManager.initializeMemberStatus(roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));

        List<Long> memberIds = room.getMembers().stream()
                .map(Member::getId).toList();
        List<UserInfo> userInfoList = new ArrayList<>();

        for (Long memberId : memberIds) {
            Member member = memberService.getMemberById(memberId);
            boolean isHost = memberId.equals(room.getHost().getId());

            log.debug("User {} ready status in response: {}", member.getUsername(), false);

            UserInfo userInfo = new UserInfo(member.getUsername(), false, isHost);
            userInfoList.add(userInfo);
        }

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