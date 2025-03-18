package com.curioussong.alsongdalsong.common.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterManager {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter add(String username, Long timeout) {
        if (emitters.containsKey(username)) {
            remove(username);
        }

        SseEmitter emitter = new SseEmitter(timeout);
        emitters.put(username, emitter);

        emitter.onCompletion(() -> remove(username));
        emitter.onTimeout(() -> remove(username));
        emitter.onError(e -> remove(username));

        return emitter;
    }

    public void remove(String username) {
        SseEmitter emitter = emitters.remove(username);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // 이미 종료된 SSE는 무시
            }
        }
    }

    public void sendToAll(String eventName, Object data) {
        List<String> deadEmitters = new CopyOnWriteArrayList<>();

        emitters.forEach((username, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (Exception e) {
                deadEmitters.add(username);
            }
        });

        // 실패한 SSE 일괄 삭제
        deadEmitters.forEach(this::remove);
    }

    public int getConnectionCount() {
        return emitters.size();
    }
}
