package com.curioussong.alsongdalsong.game.board;

import com.curioussong.alsongdalsong.game.board.enums.BoardEventType;
import com.curioussong.alsongdalsong.game.domain.InGameManager;
import com.curioussong.alsongdalsong.game.dto.board.BoardEventResponse;
import com.curioussong.alsongdalsong.game.dto.board.BoardEventResponseDTO;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardEventHandler {

    private final GameMessageSender gameMessageSender;
    private final InGameManager inGameManager;

    public BoardEventResponseDTO generateEvent(String trigger, int playerCount, String roomId) {
        boolean isSoloPlay = (playerCount == 1);

        BoardEventType eventType;
        String target = null;

        if (isSoloPlay) {
            eventType = BoardEventType.getRandomSoloEventType();
        } else {
            eventType = (target = findTieUser(trigger, roomId)) != null ? BoardEventType.OVERLAP
                    : BoardEventType.getRandomEventType();
        }

        log.debug("Generating event  trigger {}", trigger);


        if (eventType == BoardEventType.PULL) {
            target = findPullEventTarget(roomId, trigger);
            if (target == null) {
                log.info("is Not Pullable");
                eventType = BoardEventType.NOTHING;
            }
        } else if (eventType == BoardEventType.SWAP) {
            target = findSwapEventTarget(trigger, roomId);
        } else if (eventType == BoardEventType.MAGNET) {
            target = findMagnetEventTarget(trigger, roomId);
        }

        BoardEventResponse eventResponse = BoardEventResponse.builder()
                .eventType(eventType)
                .trigger(trigger)
                .target(target)
                .build();


        return BoardEventResponseDTO.builder()
                .response(eventResponse)
                .build();
    }

    public void handleEvent(String destination, String roomId, BoardEventResponseDTO eventResponseDTO) {

        try {
            // 1. 이벤트 트리거 메시지 전송
            String trigger = eventResponseDTO.getResponse().getTrigger();
            String target = eventResponseDTO.getResponse().getTarget();

            gameMessageSender.sendEventTrigger(destination, trigger);

            // 1초 대기
            Thread.sleep(1500);

            // 2. 이벤트 메시지 전송
            gameMessageSender.sendBoardEventMessage(destination, eventResponseDTO);

            // 이벤트 타입, 발생자 추출
            BoardEventResponse response = eventResponseDTO.getResponse();
            BoardEventType eventType = response.getEventType();

            log.info("Event triggered: type={}, trigger={}", eventType, trigger);

            Thread.sleep(3000);

            // 3. 이벤트 종료 메시지 전송
            gameMessageSender.sendEventEnd(destination);

            Thread.sleep(1500);

            // 4. 이벤트 효과 적용 (move 메시지 전송 포함)
            applyEventEffect(destination, roomId, eventType, trigger, target);

            Thread.sleep(1500);

        } catch (Exception e) {
            log.error("Error handling event: {}", e.getMessage(), e);
        }
    }

    private String findPullEventTarget(String roomId, String trigger) {
        Random random = new Random();
        int pullDirection = random.nextInt(2); // 0: 뒤쳐진 사람, 1: 앞선 사람

        Map<String, Integer> userScore = inGameManager.getScore(roomId);
        int triggerPosition = userScore.get(trigger);

        if (pullDirection == 0) { // 뒤쳐진 사람 당길 수 있는지 확인
            return findPullFrontTarget(userScore, triggerPosition);
        } else if (pullDirection == 1) { // 앞선 사람 당길 수 있는지 확인
            return findPullBehindTarget(userScore, triggerPosition);
        }
        return null;
    }

    private String findPullFrontTarget(Map<String, Integer> userScore, int triggerPosition) {
        String target = null;
        int targetPosition = Integer.MIN_VALUE;
        for (Map.Entry<String, Integer> entry : userScore.entrySet()) {
            int score = entry.getValue();
            if (score == 0) { // 시작점에 있으면 끌어당기지 않음
                continue;
            }
            if (score < triggerPosition && score > targetPosition) {
                target = entry.getKey();
                targetPosition = score;
            }
        }

        return target;
    }

    private String findPullBehindTarget(Map<String, Integer> userScore, int triggerPosition) {
        String target = null;
        int targetPosition = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : userScore.entrySet()) {
            int score = entry.getValue();
            if (score > triggerPosition && score < targetPosition) {
                target = entry.getKey();
                targetPosition = score;
            }
        }

        return target;
    }

    private void applyEventEffect(String destination, String roomId, BoardEventType eventType, String trigger, String target) {
        // 현재 위치 가져오기
        int currentPosition = inGameManager.getScore(roomId).getOrDefault(trigger, 0);

        try {
            switch (eventType) {
                case PLUS -> handlePlusEvent(destination, roomId, trigger, currentPosition);

                case MINUS -> handleMinusEvent(destination, roomId, trigger, currentPosition);

                case PULL -> handlePullEvent(roomId, destination, eventType, trigger, target);

                case CLOVER -> handleCloverEvent(destination, roomId, trigger, currentPosition);

                case BOMB -> handleBombEvent(destination, roomId, trigger, currentPosition);

                case SWAP -> handleSwapEvent(destination, roomId, trigger, target);

                case WARP -> handleWarpEvent(destination, roomId, trigger);

                case MAGNET -> handleMagnetEvent(destination, roomId, trigger, target);

                case OVERLAP -> handleOverlapEvent(destination, roomId, trigger, target);

                case NOTHING -> log.info("No effect for user {}", trigger);
                default -> log.info("Case default, No effect for user {}", trigger);
            }

        } catch (Exception e) {
            log.error("Error processing event {}: {}", eventType, e.getMessage(), e);
        }
    }

    // 랜덤한 플레이어와 자리 교체
    private void handleSwapEvent(String destination, String roomId, String trigger, String target) {
        Map<String, Integer> scores = inGameManager.getScore(roomId);

        // 위치 스왑
        int temp = scores.get(target);
        scores.put(target, scores.get(trigger));
        scores.put(trigger, temp);

        // 위치 정보 전송
        gameMessageSender.sendUserPosition(destination, trigger, scores.get(trigger));
        gameMessageSender.sendUserPosition(destination, target, scores.get(target));
    }

    private String findSwapEventTarget(String trigger, String roomId) {
        // 방에 있는 플레이어 중 하나 선택
        Map<String, Integer> scores = inGameManager.getScore(roomId);

        List<String> others = new ArrayList<>();

        // 나를 제외한 유저 찾기
        for (String name : scores.keySet()) {
            if (!name.equals(trigger)) {
                others.add(name);
            }
        }

        Random random = new Random();
        return others.get(random.nextInt(others.size()));
    }

    private void handleWarpEvent(String destination, String roomId, String trigger) {
        int newPosition = ThreadLocalRandom.current().nextInt(0, 16);
        log.debug("nowPosition:{}", newPosition);
        updatePositionAndSendMessage(destination, roomId, trigger, newPosition);
    }

    private void handleBombEvent(String destination, String roomId, String trigger, int currentPosition) {
        int bombMove = 5;
        int newPosition = Math.max(0, currentPosition - bombMove);
        updatePositionAndSendMessage(destination, roomId, trigger, newPosition);
    }

    private void handleCloverEvent(String destination, String roomId, String trigger, int currentPosition) {
        int cloverMove = 5;
        int newPosition = currentPosition + cloverMove;
        updatePositionAndSendMessage(destination, roomId, trigger, newPosition);
    }

    private void handlePlusEvent(String destination, String roomId, String trigger, int currentPosition) {
        // 1~2칸 앞으로 이동
        int plusAmount = ThreadLocalRandom.current().nextInt(1, 3);
        int newPosition = currentPosition + plusAmount;
        log.info("PLUS event: {} moved forward by {} steps (new position: {})", trigger, plusAmount, newPosition);
        updatePositionAndSendMessage(destination, roomId, trigger, newPosition);
    }

    private void handleMinusEvent(String destination, String roomId, String trigger, int currentPosition) {
        // 1칸 or 2칸 뒤로 이동
        int minusAmount = ThreadLocalRandom.current().nextInt(1, 3);
        int newPosition = Math.max(0, currentPosition - minusAmount);
        log.info("MINUS event: {} moved backward by {} steps (new position: {})", trigger, minusAmount, newPosition);
        updatePositionAndSendMessage(destination, roomId, trigger, newPosition);
    }

    private void updatePositionAndSendMessage(String destination, String roomId, String trigger, int newPosition) {
        // 새 위치 업데이트 (RoomManager 호출)
        inGameManager.getScore(roomId).put(trigger, newPosition);
        // 위치 변경 메시지 전송 (GameMessageSender 호출)
        gameMessageSender.sendUserPosition(destination, trigger, newPosition);
    }

    private void handlePullEvent(String roomId, String destination, BoardEventType eventType, String trigger, String target) {
        Map<String, Integer> userScore = inGameManager.getScore(roomId);
        int targetPosition = userScore.get(trigger);
        updatePositionAndSendMessage(destination, roomId, target, targetPosition);
    }

    private void handleMagnetEvent(String destination, String roomId, String trigger, String target) {
        int targetPosition = inGameManager.getScore(roomId).get(target);
        updatePositionAndSendMessage(destination, roomId, trigger, targetPosition);
    }

    private String findMagnetEventTarget(String trigger, String roomId) {
        Map<String, Integer> userScore = inGameManager.getScore(roomId);
        int triggerPosition = userScore.get(trigger);
        return userScore.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(trigger))
                .min(Comparator.comparingInt(entry -> Math.abs(entry.getValue() - triggerPosition)))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private String findTieUser(String trigger, String roomId) {
        Map<String, Integer> userScores = inGameManager.getScore(roomId);
        int currentWinnerScore = userScores.get(trigger);

        List<String> tieUsers = userScores.entrySet().stream()
                .filter(entry -> (entry.getValue() == currentWinnerScore) && !entry.getKey().equals(trigger))
                .map(Map.Entry::getKey)
                .toList();

        return tieUsers.isEmpty() ? null
                : tieUsers.size() == 1 ? tieUsers.get(0)
                : pickRandomTieUser(tieUsers);
    }

    private String pickRandomTieUser(List<String> tieUsers) {
        SecureRandom random = new SecureRandom();
        return tieUsers.get(random.nextInt(tieUsers.size()));
    }

    private void handleOverlapEvent(String destination, String roomId, String trigger, String target) {
        int triggerPreviousPosition = inGameManager.getScore(roomId).get(trigger) - inGameManager.getUserMovement(roomId).get(trigger);
        updatePositionAndSendMessage(destination, roomId, target, triggerPreviousPosition);
    }
}
