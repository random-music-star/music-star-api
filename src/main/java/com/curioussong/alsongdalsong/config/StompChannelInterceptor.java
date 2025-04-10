package com.curioussong.alsongdalsong.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

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
            case CONNECT -> handleConnect(accessor);
            case SEND -> handleSend(accessor, message);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String userId = accessor.getFirstNativeHeader("Authorization");
        accessor.setUser(() -> userId);
    }

    private void handleSend(StompHeaderAccessor accessor, Message<?> message) {
        Object rawPayload = message.getPayload();
        if (rawPayload instanceof byte[]) {
            String json = new String((byte[]) rawPayload, StandardCharsets.UTF_8);

            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(json);

                if (root.has("type") && "roundStartReceived".equals(root.get("type").asText())) {
                    log.info("노래 재생 시작 시간 : {}", LocalDateTime.now());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
