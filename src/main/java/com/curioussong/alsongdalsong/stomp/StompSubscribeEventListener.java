package com.curioussong.alsongdalsong.stomp;

import com.curioussong.alsongdalsong.game.dto.test.TestResponse;
import com.curioussong.alsongdalsong.game.dto.test.TestResponseDTO;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfoResponseDTO;
import com.curioussong.alsongdalsong.game.service.GameService;
import com.curioussong.alsongdalsong.room.dto.RefuseEnterResponse;
import com.curioussong.alsongdalsong.room.dto.RefuseEnterResponseDTO;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import com.curioussong.alsongdalsong.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class StompSubscribeEventListener implements ApplicationListener<SessionSubscribeEvent> {

    private final SimpMessagingTemplate messagingTemplate;
    private final GameService gameService;
    private final RoomService roomService;
    private final SessionManager sessionManager;

    // 토픽 구독을 감지하는 메서드
    @Override
    public void onApplicationEvent(SessionSubscribeEvent event) {
        log.info("Subscribe onApplicationEvent {}", event.toString());
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId(); // 각 클라이언트 연결 식별하는 고유 id

        if (destination != null && sessionId != null && destination.matches("^/topic/channel/\\d+/room/\\d+$")) {
            String userName = getUsernameFromHeader(accessor);
            if (userName == null) {
                // Todo : 헤더로 사용자 이름을 전달받지 못한 경우에 대한 예외 처리
            }
            log.info("username {}", userName);

            Pattern pattern = Pattern.compile("^/topic/channel/(\\d+)/room/(\\d+)$");
            Matcher matcher = pattern.matcher(destination);

            if (matcher.find()) { //
                Long channelId = Long.parseLong(matcher.group(1));
                Long roomId = Long.parseLong(matcher.group(2));
                log.info("room id {}, It's work when subscribe the topic", roomId);
                // 방이 가득 찼거나, 게임 진행 중이면 참가 불가.
                if (roomService.isRoomFull(roomId) || roomService.isRoomInProgress(roomId)) {
                    sendRefuseMessage(destination, userName);
                    return;
                }
                roomService.joinRoom(roomId, sessionId, userName);
                sessionManager.addSessionId(sessionId, channelId, roomId, userName);
                sendRoomInfoAndUserInfoToSubscriber(destination); // 방 입장 시, 해당 방에 입장한 사용자들에게 방 정보와 사용자 목록을 내려줌.
            }
        }
    }

    public void sendRoomInfoAndUserInfoToSubscriber(String destination) {
        Pattern pattern = Pattern.compile("^/topic/channel/(\\d+)/room/(\\d+)$");
        Matcher matcher = pattern.matcher(destination);
        if (matcher.matches()) {
            Long channelId = Long.parseLong(matcher.group(1));
            Long roomId = Long.parseLong(matcher.group(2));
            gameService.sendRoomInfoAndUserInfoToSubscriber(channelId, roomId);
        }
    }

    private String getUsernameFromHeader(StompHeaderAccessor accessor) {
        if (accessor.getNativeHeader("Authorization") != null && !accessor.getNativeHeader("Authorization").isEmpty()) {
            return accessor.getNativeHeader("Authorization").get(0);
        }
        return null;
    }

    private void sendRefuseMessage(String destination, String userName) {
        messagingTemplate.convertAndSend(destination, RefuseEnterResponseDTO.builder()
                .type("refuseEnter")
                .response(RefuseEnterResponse.builder()
                        .refusedUser(userName)
                        .build())
                .build());
    }
}
