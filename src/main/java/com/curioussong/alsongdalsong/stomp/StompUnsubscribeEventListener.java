package com.curioussong.alsongdalsong.stomp;

import com.curioussong.alsongdalsong.game.service.GameService;
import com.curioussong.alsongdalsong.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Pair;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class StompUnsubscribeEventListener implements ApplicationListener<SessionUnsubscribeEvent> {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;
    private final GameService gameService;
    private final SessionManager sessionManager;

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        log.info("Session disconnect event: {}", event);
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        log.info("사용자 세션 종료: {}", sessionId);

        handleLeavingRoom(sessionId);
    }

    @Override
    public void onApplicationEvent(SessionUnsubscribeEvent event) {
        log.info("Unsubscribe: {}", event);
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        // 세션이 방에 연결되어 있는지 확인
        if (sessionManager.getSessionRoomMap().containsKey(sessionId)) {
            Map<String, Pair<Long, Long>> userSessionInfo = sessionManager.getSessionRoomMap().get(sessionId);
            String userName = userSessionInfo.keySet().stream().findFirst().orElse(null);
            Long channelId = userSessionInfo.get(userName).getFirst();
            Long roomId = userSessionInfo.get(userName).getSecond();

            // roomId가 -1L이 아니면 방에 있는 상태
            if (roomId != -1L) {
                log.debug("방 토픽 구독 해제 감지: 방ID={}, 사용자={}", roomId, userName);
                // 방 나가기 처리
                roomService.leaveRoom(roomId, userName);
                sessionManager.removeSessionId(sessionId);
                sendRoomInfoAndUserInfoToSubscribers(channelId, roomId);
            }
        }
    }

    private void handleLeavingRoom(String sessionId) {
        if (sessionManager.getSessionRoomMap().containsKey(sessionId)) {
            Map<String, Pair<Long, Long>> userSessionInfo = sessionManager.getSessionRoomMap().get(sessionId);
            String userName = userSessionInfo.keySet().stream().findFirst().orElse(null);
            Long channelId = userSessionInfo.get(userName).getFirst();
            Long roomId = userSessionInfo.get(userName).getSecond();

            // roomId가 -1L이 아닌 경우에만 방 나가기 처리
            if (roomId != -1L) {
                roomService.leaveRoom(roomId, userName);
                // 채널은 유지하고 방만 나가는 경우 처리 (sessionManager 업데이트)
                sessionManager.removeSessionId(sessionId);
                sendRoomInfoAndUserInfoToSubscribers(channelId, roomId);
            }
        }
    }

    public void sendRoomInfoAndUserInfoToSubscribers(Long channelId, Long roomId) {
        gameService.sendRoomInfoAndUserInfoToSubscriber(channelId, roomId);
    }
}
