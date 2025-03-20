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
    private final Map<String, Set<String>> subscribedUsers = new ConcurrentHashMap<>();
    private final GameService gameService;
    private final RoomService roomService;

    // 토픽 구독을 감지하는 메서드
    @Override
    public void onApplicationEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId(); // 각 클라이언트 연결 식별하는 고유 id

        if (destination != null && sessionId != null && destination.matches("^/topic/channel/\\d+/room/\\d+$")) {
            String userName = "";
            if (accessor.getNativeHeader("Authorization") != null && !accessor.getNativeHeader("Authorization").isEmpty()) {
                userName = accessor.getNativeHeader("Authorization").get(0);
            }
            log.info("username {}", userName);
            log.info("accessor {}", accessor);

            // 정규식: 마지막 숫자 추출
            Pattern pattern = Pattern.compile(".*/(\\d+)$");
            Matcher matcher = pattern.matcher(destination);

            if (matcher.find()) { //
                Long roomId = Long.valueOf(matcher.group(1)); // 마지막 숫자 추출
                log.info("room id {}, It's work when subscribe the topic", roomId);
                // 방이 가득 찼거나, 게임 진행 중이면 참가 불가.
                if (roomService.isRoomFull(roomId) || roomService.isRoomInProgress(roomId)) {
                    messagingTemplate.convertAndSend(destination, RefuseEnterResponseDTO.builder()
                                    .type("refuseEnter")
                                    .response(RefuseEnterResponse.builder()
                                            .refusedUser(userName)
                                            .build())
                            .build());
                    return;
                }
                roomService.joinRoom(roomId, sessionId, userName);
//                log.info("Extracted Room ID: {}", roomId);
            }
            sendRoomInfoAndUserInfoToSubscriber(destination); // 방 입장 시, 해당 방에 입장한 사용자들에게 방 정보와 사용자 목록을 내려줌.
        }
    }

    public Set<String> getSubscribedUsers(String destination) {
        return subscribedUsers.getOrDefault(destination, Collections.emptySet());
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
}
