package com.curioussong.alsongdalsong.common.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitterManager {

    private final Map<String, SseEmitter> selectorEmitters = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> channelEmitters = new ConcurrentHashMap<>();

    private final Map<Long, Set<String>> channelSubscribers = new ConcurrentHashMap<>();
    private final Set<String> channelSelectorUsers = ConcurrentHashMap.newKeySet();

    public SseEmitter addToChannel(String sessionId, Long channelId, Long timeout) {
        cleanupChannelSession(sessionId);
        cleanupSelectorSession(sessionId);

        SseEmitter emitter = new SseEmitter(timeout);
        channelEmitters.put(sessionId, emitter);
        channelSubscribers.computeIfAbsent(channelId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        log.debug("채널 {} 로비 SSE 연결 추가: {}, 총 연결 수: {}", channelId, sessionId, channelEmitters.size());

        return registerChannelEmitterCleanup(sessionId, emitter);
    }

    public SseEmitter addForChannelSelection(String sessionId, long timeout) {
        cleanupSelectorSession(sessionId);
        cleanupChannelSession(sessionId);

        SseEmitter emitter = new SseEmitter(timeout);
        selectorEmitters.put(sessionId, emitter);
        channelSelectorUsers.add(sessionId);

        log.debug("채널 선택 화면 SSE 연결 추가: {}, 총 연결 수: {}", sessionId, selectorEmitters.size());

        return registerSelectorEmitterCleanup(sessionId, emitter);
    }

    private void cleanupSelectorSession(String sessionId) {
        SseEmitter emitter = selectorEmitters.remove(sessionId);
        if (emitter != null) {
            try {
                emitter.complete();
                log.debug("채널 목록 SSE 연결 제거: {}", sessionId);
            } catch (Exception e) {
                log.warn("채널 목록 SSE 종료 중 에러: {}", sessionId, e);
            }
        }
        channelSelectorUsers.remove(sessionId);
    }

    private void cleanupChannelSession(String sessionId) {
        channelSubscribers.forEach((channelId, subscribers) -> {
            subscribers.remove(sessionId);
            if (subscribers.isEmpty()) {
                channelSubscribers.remove(channelId);
            }
        });

        SseEmitter emitter = channelEmitters.remove(sessionId);
        if (emitter != null) {
            try {
                emitter.complete();
                log.debug("채널 SSE 연결 제거: {}", sessionId);
            } catch (Exception e) {
                log.warn("채널 SSE 종료 중 에러: {}", sessionId, e);
            }
        }
    }

    private SseEmitter registerChannelEmitterCleanup(String sessionId, SseEmitter emitter) {
        emitter.onCompletion(() -> cleanupChannelSession(sessionId));
        emitter.onTimeout(() -> cleanupChannelSession(sessionId));
        emitter.onError(e -> cleanupChannelSession(sessionId));
        return emitter;
    }

    private SseEmitter registerSelectorEmitterCleanup(String sessionId, SseEmitter emitter) {
        emitter.onCompletion(() -> cleanupSelectorSession(sessionId));
        emitter.onTimeout(() -> cleanupSelectorSession(sessionId));
        emitter.onError(e -> cleanupSelectorSession(sessionId));
        return emitter;
    }

    @Async
    public void sendToChannel(Long channelId, String eventName, Object data) {
        Set<String> subscribers = channelSubscribers.get(channelId);
        if (subscribers == null || subscribers.isEmpty()) return;

        log.debug("채널 {} 구독자에게 '{}' 이벤트 전송", channelId, eventName);

        for (String sessionId : subscribers) {
            SseEmitter emitter = channelEmitters.get(sessionId);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                } catch (Exception e) {
                    log.warn("채널 이벤트 전송 실패: {}, {}", sessionId, e.getMessage());
                    cleanupChannelSession(sessionId);
                }
            }
        }
    }

    @Async
    public void sendToChannelSelectors(String eventName, Object data) {
        for (String sessionId : channelSelectorUsers) {
            SseEmitter emitter = selectorEmitters.get(sessionId);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                } catch (Exception e) {
                    log.warn("채널 목록 이벤트 전송 실패: {}, {}", sessionId, e.getMessage());
                    cleanupSelectorSession(sessionId);
                }
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    public void sendHeartbeat() {
        selectorEmitters.forEach((sessionId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("HEARTBEAT").data(""));
            } catch (Exception e) {
                cleanupSelectorSession(sessionId);
            }
        });

        channelEmitters.forEach((sessionId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("HEARTBEAT").data(""));
            } catch (Exception e) {
                cleanupChannelSession(sessionId);
            }
        });
    }
}
