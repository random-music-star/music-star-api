package com.curioussong.alsongdalsong.game.board;

import com.curioussong.alsongdalsong.game.board.enums.BoardEventType;
import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardEventHandler {

    private final GameMessageSender gameMessageSender;
    private final RoomManager roomManager;

    public void handleBoardEvent(BoardEventType eventType, String triggerUser, Long roomId, String destination, int positionBeforeMove) {
        // overlap 여부 판단
        Map<String, Integer> scoreMap = roomManager.getRoomInfo(roomId).getScore();
        int nowUserScore = scoreMap.get(triggerUser);
        List<String> overlappedUsers = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : scoreMap.entrySet()) {
            if (entry.getValue().equals(nowUserScore) && !entry.getKey().equals(triggerUser)) {
                overlappedUsers.add(entry.getKey());
            }
        }
        // 겹치는 모든 사용자의 위치를 positionBeforeMove로 이동시키기
        if (!overlappedUsers.isEmpty()) {
            for (String overlappedUser : overlappedUsers) {
                gameMessageSender.sendUserPosition(destination, overlappedUser, positionBeforeMove);
            }
        } else { // 겹치는 사용자 없으면 이벤트 발생 (일단 무조건 발생)
            // 일단 무조건 당겨오기
            if (eventType == BoardEventType.PULL) {
                handlePullEvent(eventType, triggerUser, roomId, destination);
            }
        }
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
                gameMessageSender.sendEvent(destination, BoardEventType.NOTHING, triggerUser, null);
            } else {
                scores.put(targetUser, triggerUserPosition);
                gameMessageSender.sendEvent(destination, eventType, triggerUser, targetUser);

                gameMessageSender.sendUserPosition(destination, targetUser, triggerUserPosition);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
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
                gameMessageSender.sendEvent(destination, BoardEventType.NOTHING, triggerUser, null);
            } else {
                scores.put(targetUser, triggerUserPosition);
                gameMessageSender.sendEvent(destination, eventType, triggerUser, targetUser);

                gameMessageSender.sendUserPosition(destination, targetUser, triggerUserPosition);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
