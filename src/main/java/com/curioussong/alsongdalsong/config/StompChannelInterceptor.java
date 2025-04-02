package com.curioussong.alsongdalsong.config;

import com.curioussong.alsongdalsong.room.event.EnterRoomEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor.getCommand() == null) {
            log.info("Received a heartbeat from session: {}", accessor.getSessionId());
            return message;
        }

        switch (accessor.getCommand()) {
//            case CONNECT:
            case SUBSCRIBE: handleSubscribe(accessor);
        }

        return message;
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String userId = accessor.getFirstNativeHeader("Authorization");
        accessor.setUser(() -> userId);

        String destination = accessor.getDestination();

        Pattern roomPattern = Pattern.compile("^/topic/channel/(\\d+)/room/([A-Z0-9]{26})$");
        Matcher roomMatcher = roomPattern.matcher(destination);

        if (roomMatcher.matches()) {
            Long channelId = Long.parseLong(roomMatcher.group(1));
            String roomId = roomMatcher.group(2);

            applicationEventPublisher.publishEvent(new EnterRoomEvent(roomId));
        }
    }

//    private void handleConnect(StompHeaderAccessor accessor) {
//        String userId = accessor.getFirstNativeHeader("Authorization");
//        accessor.setUser(() -> userId);
//    }

    private boolean validate(StompHeaderAccessor accessor) {
        return false;
    }
}
