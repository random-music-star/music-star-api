package com.curioussong.alsongdalsong.common.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterManager {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter add(String sessionId, Long timeout) {
        remove(sessionId);

        SseEmitter emitter = new SseEmitter(timeout);
        emitters.put(sessionId, emitter);

        emitter.onCompletion(() -> remove(sessionId));
        emitter.onTimeout(() -> remove(sessionId));
        emitter.onError(e -> remove(sessionId));

        return emitter;
    }

    public void remove(String sessionId) {
        SseEmitter emitter = emitters.remove(sessionId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    @Async
    public void sendToAll(String eventName, Object data) {
        emitters.forEach((key, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (Exception e) {
                remove(key);
            }
        });
    }
}