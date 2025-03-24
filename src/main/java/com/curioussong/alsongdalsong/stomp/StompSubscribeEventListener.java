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

        if(destination == null || sessionId == null) {
            return;
        }

        String userName = getUsernameFromHeader(accessor);
        if(userName == null) {
            // Todo : 헤더로 사용자 이름을 전달받지 못한 경우에 대한 예외 처리
            return;
        }
        log.debug("username {}", userName);

        // 채널 토픽 구독 패턴 (예: /topic/channel/1)
        Pattern channelPattern = Pattern.compile("^/topic/channel/(\\d+)$");
        Matcher channelMatcher = channelPattern.matcher(destination);

        // 방 토픽 구독 패턴 (예: /topic/channel/1/room/2)
        Pattern roomPattern = Pattern.compile("^/topic/channel/(\\d+)/room/(\\d+)$");
        Matcher roomMatcher = roomPattern.matcher(destination);

        if (channelMatcher.find()) {
            Long channelId = Long.parseLong(channelMatcher.group(1));
            log.debug("channel id {}, 채널 토픽 구독", channelId);

            // 채널 입장을 위한 세션 정보 등록 (roomId는 -1로)
            sessionManager.addSessionId(sessionId, channelId, -1L, userName);
        }

        // 방 토픽 구독인 경우 (방 입장)
        else if (roomMatcher.find()) {
            Long channelId = Long.parseLong(roomMatcher.group(1));
            Long roomId = Long.parseLong(roomMatcher.group(2));

            log.debug("room id {}, It's work when subscribe the topic", roomId);

            // 방이 가득 찼거나, 게임 진행 중이면 참가 불가
            if (roomService.isRoomFull(roomId) || roomService.isRoomInProgress(roomId)) {
                sendRefuseMessage(destination, userName);
                return;
            }
            roomService.joinRoom(roomId, sessionId, userName);
            sessionManager.addSessionId(sessionId, channelId, roomId, userName);
            sendRoomInfoAndUserInfoToSubscriber(channelId, roomId);
        }
    }

    public void sendRoomInfoAndUserInfoToSubscriber(Long channelId, Long roomId) {
        gameService.sendRoomInfoAndUserInfoToSubscriber(channelId, roomId);
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
