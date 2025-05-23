package com.curioussong.alsongdalsong.stomp;

import com.curioussong.alsongdalsong.channel.enums.ChannelEventType;
import com.curioussong.alsongdalsong.channel.event.ChannelStatusChangedEvent;
import com.curioussong.alsongdalsong.channel.event.ChannelUserUpdateEvent;
import com.curioussong.alsongdalsong.game.service.GameService;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.dto.MemberStatusDTO;
import com.curioussong.alsongdalsong.member.service.MemberService;
import com.curioussong.alsongdalsong.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Pair;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class StompUnsubscribeEventListener implements ApplicationListener<SessionUnsubscribeEvent> {

    private final RoomService roomService;
    private final GameService gameService;
    private final SessionManager sessionManager;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final MemberService memberService;

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
            Map<String, Pair<Long, String>> userSessionInfo = sessionManager.getSessionRoomMap().get(sessionId);
            String userName = userSessionInfo.keySet().stream().findFirst().orElse(null);
            Long channelId = userSessionInfo.get(userName).getFirst();
            String roomId = userSessionInfo.get(userName).getSecond();

            // roomId가 -1L이 아니면 방에 있는 상태
            if (!("-1").equals(roomId)) {
                log.info("방 토픽 구독 해제 감지: 방ID={}, 사용자={}", roomId, userName);
                // 방 나가기 처리
                roomService.leaveRoom(channelId, roomId, userName);
                sessionManager.removeSessionId(sessionId);
//                sendRoomInfoAndUserInfoToSubscribers(channelId, roomId);
            }
        }
    }

    private void handleLeavingRoom(String sessionId) {
        if (sessionManager.getSessionRoomMap().containsKey(sessionId)) {
            Map<String, Pair<Long, String>> userSessionInfo = sessionManager.getSessionRoomMap().get(sessionId);
            String userName = userSessionInfo.keySet().stream().findFirst().orElse(null);
            Long channelId = userSessionInfo.get(userName).getFirst();
            String roomId = userSessionInfo.get(userName).getSecond();

            // roomId가 -1L이 아닌 경우에만 방 나가기 처리
            if (!("-1").equals(roomId)) {
                roomService.leaveRoom(channelId, roomId, userName);
                // 채널은 유지하고 방만 나가는 경우 처리 (sessionManager 업데이트)
                sessionManager.removeSessionId(sessionId);
//                sendRoomInfoAndUserInfoToSubscribers(channelId, roomId);
            }
            memberService.leaveChannel(userName);
            sessionManager.userLeaveChannel(channelId, userName);
            int playerCount = sessionManager.getChannelUserCount(channelId);
            applicationEventPublisher.publishEvent(new ChannelStatusChangedEvent(channelId, ChannelEventType.LEAVE, playerCount));

            Member member = memberService.getMemberByToken(userName);
            MemberStatusDTO memberStatus = MemberStatusDTO.from(member);

            applicationEventPublisher.publishEvent(new ChannelUserUpdateEvent(channelId, memberStatus, ChannelEventType.LEAVE));
        }
    }
}
