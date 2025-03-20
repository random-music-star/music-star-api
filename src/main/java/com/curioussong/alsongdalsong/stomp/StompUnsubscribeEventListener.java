package com.curioussong.alsongdalsong.stomp;

import com.curioussong.alsongdalsong.game.service.GameService;
import com.curioussong.alsongdalsong.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class StompUnsubscribeEventListener implements ApplicationListener<SessionUnsubscribeEvent> {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;
    private final GameService gameService;
    private final SessionRoomMap sessionRoomMap;

    @Override
    public void onApplicationEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination(); // 구독했던 토픽
        String userName = "";

        // 헤더에서 사용자 정보 가져오기 (Authorization 사용 예시)
        if (accessor.getNativeHeader("Authorization") != null && !accessor.getNativeHeader("Authorization").isEmpty()) {
            userName = accessor.getNativeHeader("Authorization").get(0);
        }

        if (sessionRoomMap.getSessionRoomMap().get(sessionId) != null) {
            log.info("leave room session {}", sessionId);
            roomService.leaveRoom(sessionId);
        }

//        if (destination != null && destination.matches("^/topic/channel/\\d+/room/\\d+$")) {
//            Pattern pattern = Pattern.compile(".*/(\\d+)$");
//            Matcher matcher = pattern.matcher(destination);
//
//            if (matcher.find()) {
//                Long roomId = Long.valueOf(matcher.group(1));
//                log.info("User {} left room {}", userName, roomId);
//
//                // 사용자를 방에서 제거
//                roomService.leaveRoom(sessionId, roomId, userName);
//
//                // 사용자 목록 갱신 및 알림 전송
////                sendRoomInfoAndUserInfoToSubscribers(destination);
//            }
//        }
    }

    public void sendRoomInfoAndUserInfoToSubscribers(String destination) {
        Pattern pattern = Pattern.compile("^/topic/channel/(\\d+)/room/(\\d+)$");
        Matcher matcher = pattern.matcher(destination);
        if (matcher.matches()) {
            Long channelId = Long.parseLong(matcher.group(1));
            Long roomId = Long.parseLong(matcher.group(2));
            gameService.sendRoomInfoAndUserInfoToSubscriber(channelId, roomId);
        }
    }
}
