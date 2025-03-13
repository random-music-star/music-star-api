package com.curioussong.alsongdalsong.common.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterManager {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter add(String type, Long timeout) {
        SseEmitter emitter = new SseEmitter(timeout);

        List<SseEmitter> typeEmitters = emitters.computeIfAbsent(
                type, k -> new CopyOnWriteArrayList<>());
        typeEmitters.add(emitter);

        emitter.onCompletion(() -> remove(type, emitter));
        emitter.onTimeout(() -> remove(type, emitter));
        emitter.onError(e -> remove(type, emitter));

        return emitter;
    }

    public void remove(String type, SseEmitter emitter) {
        List<SseEmitter> typeEmitters = emitters.get(type);
        if (typeEmitters != null) {
            typeEmitters.remove(emitter);
        }
    }

    public void sendToAll(String type, String eventName, Object data) {
        List<SseEmitter> typeEmitters = emitters.get(type);
        if (typeEmitters == null) return;

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : typeEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        typeEmitters.removeAll(deadEmitters);
    }

    public int getConnectionCount(String type) {
        List<SseEmitter> typeEmitters = emitters.get(type);
        return typeEmitters != null ? typeEmitters.size() : 0;
    }
}