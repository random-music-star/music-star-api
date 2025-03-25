package com.curioussong.alsongdalsong.game.board;

import com.curioussong.alsongdalsong.game.board.enums.BoardEventType;
import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.dto.board.BoardEventResponse;
import com.curioussong.alsongdalsong.game.dto.board.BoardEventResponseDTO;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardEventHandler {

    private final RoomManager roomManager;
    private final GameMessageSender gameMessageSender;

    public BoardEventResponseDTO generateEvent(String trigger) {

        BoardEventType eventType = BoardEventType.getRandomEventType();
        log.debug("Generating event  trigger {}", trigger);

        String target = null;

        BoardEventResponse eventResponse = BoardEventResponse.builder()
                .eventType(eventType)
                .trigger(trigger)
                .target(target)
                .build();

        return BoardEventResponseDTO.builder()
                .response(eventResponse)
                .build();
    }

    public void handleEvent(String destination, Long roomId, BoardEventResponseDTO eventResponseDTO) {

        try {
            // 1. 이벤트 트리거 메시지 전송
            String trigger = eventResponseDTO.getResponse().trigger();

            gameMessageSender.sendEventTrigger(destination, trigger);

            // 1초 대기
            Thread.sleep(1500);

            // 2. 이벤트 메시지 전송
            gameMessageSender.sendBoardEventMessage(destination, eventResponseDTO);

            // 이벤트 타입, 발생자 추출
            BoardEventResponse response = eventResponseDTO.getResponse();
            BoardEventType eventType = response.eventType();

            log.info("Event triggered: type={}, trigger={}", eventType, trigger);

            // 3. 이벤트 효과 적용 (move 메시지 전송 포함)
            applyEventEffect(destination, roomId, eventType, trigger);

            Thread.sleep(1500);

            // 4. 이벤트 종료 메시지 전송
            gameMessageSender.sendEventEnd(destination);

            Thread.sleep(1500);

        } catch (Exception e) {
            log.error("Error handling event: {}", e.getMessage(), e);
        }
    }

    private void applyEventEffect(String destination, Long roomId, BoardEventType eventType, String trigger) {
        // 현재 위치 가져오기
        int currentPosition = roomManager.getRoomInfo(roomId).getScore().getOrDefault(trigger, 0);

        try {
            switch (eventType) {
                case PLUS:
                    handlePlusEvent(destination, roomId, trigger, currentPosition);
                    break;

                case MINUS:
                    handleMinusEvent(destination, roomId, trigger, currentPosition);
                    break;

                case PULL:
                    handlePullEvent(eventType, trigger, roomId, destination);
                case NOTHING:
                default:
                    log.info("No effect for user {}", trigger);
            }

        } catch (Exception e) {
            log.error("Error processing event {}: {}", eventType, e.getMessage(), e);
        }
    }

    private void handlePlusEvent(String destination, Long roomId, String trigger, int currentPosition) {
        // 1~2칸 앞으로 이동
        int plusAmount = ThreadLocalRandom.current().nextInt(1, 3);
        int newPosition = currentPosition + plusAmount;
        log.info("PLUS event: {} moved forward by {} steps (new position: {})", trigger, plusAmount, newPosition);
        updatePositionAndSendMessage(destination, roomId, trigger, newPosition);
    }

    private void handleMinusEvent(String destination, Long roomId, String trigger, int currentPosition) {
        // 1칸 or 2칸 뒤로 이동
        int minusAmount = ThreadLocalRandom.current().nextInt(1, 3);
        int newPosition = Math.max(0, currentPosition - minusAmount);
        log.info("MINUS event: {} moved backward by {} steps (new position: {})", trigger, minusAmount, newPosition);
        updatePositionAndSendMessage(destination, roomId, trigger, newPosition);
    }

    private void updatePositionAndSendMessage(String destination, Long roomId, String trigger, int newPosition) {
        // 새 위치 업데이트 (RoomManager 호출)
        roomManager.getRoomInfo(roomId).getScore().put(trigger, newPosition);
        // 위치 변경 메시지 전송 (GameMessageSender 호출)
        gameMessageSender.sendUserPosition(destination, trigger, newPosition);
    }

    private void handlePullEvent(BoardEventType eventType, String triggerUser, Long roomId, String destination) {
        Random random = new Random();
        //        int randomInt = random.nextInt(2);
        int randomInt = 1;
        Map<String, Integer> scores = roomManager.getRoomInfo(roomId).getScore();
        int triggerUserPosition = scores.get(triggerUser);
        String targetUser = null;

        // 앞 사람 중 가장 가까운 사람 당겨오기
        if (randomInt == 0) {
            int targetUserPosition = Integer.MAX_VALUE;
            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                int score = entry.getValue();
                if (score > triggerUserPosition) {
                    if (score < targetUserPosition) {
                        targetUserPosition = score;
                        targetUser = entry.getKey();
                    }
                }
            }

            // 나보다 앞선 사람이 없음
            if (targetUser == null) {
                BoardEventResponse response = BoardEventResponse.builder()
                        .eventType(BoardEventType.NOTHING)
                        .trigger(triggerUser)
                        .build();

                BoardEventResponseDTO responseDTO = BoardEventResponseDTO.builder()
                        .response(response)
                        .build();

                gameMessageSender.sendBoardEventMessage(destination, responseDTO);
            } else {
                scores.put(targetUser, triggerUserPosition);
                BoardEventResponse response = BoardEventResponse.builder()
                        .eventType(BoardEventType.NOTHING)
                        .trigger(triggerUser)
                        .target(targetUser)
                        .build();

                BoardEventResponseDTO responseDTO = BoardEventResponseDTO.builder()
                        .response(response)
                        .build();

                gameMessageSender.sendBoardEventMessage(destination, responseDTO);

                gameMessageSender.sendUserPosition(destination, targetUser, triggerUserPosition);

            }
        } else { // 뒷 사람 중 가장 가까운 사람 당겨오기
            int targetUserPosition = Integer.MIN_VALUE;
            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                int score = entry.getValue();
                if (score < triggerUserPosition) {
                    if (score > targetUserPosition) {
                        targetUserPosition = score;
                        targetUser = entry.getKey();
                    }
                }
            }

            // 나보다 뒤쳐진 사람이 없음
            if (targetUser == null) {
                BoardEventResponse response = BoardEventResponse.builder()
                        .eventType(BoardEventType.NOTHING)
                        .trigger(triggerUser)
                        .build();

                BoardEventResponseDTO responseDTO = BoardEventResponseDTO.builder()
                        .response(response)
                        .build();

                gameMessageSender.sendBoardEventMessage(destination, responseDTO);
            } else {
                scores.put(targetUser, triggerUserPosition);
                BoardEventResponse response = BoardEventResponse.builder()
                        .eventType(BoardEventType.NOTHING)
                        .trigger(triggerUser)
                        .target(targetUser)
                        .build();

                BoardEventResponseDTO responseDTO = BoardEventResponseDTO.builder()
                        .response(response)
                        .build();

                gameMessageSender.sendBoardEventMessage(destination, responseDTO);

                gameMessageSender.sendUserPosition(destination, targetUser, triggerUserPosition);
            }
        }
    }
}
