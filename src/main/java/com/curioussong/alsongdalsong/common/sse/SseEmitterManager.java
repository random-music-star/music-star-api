package com.curioussong.alsongdalsong.common.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitterManager {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter add(String sessionId, Long timeout) {
        if (emitters.containsKey(sessionId)) {
            log.info("기존 SSE 연결 제거: sessionId={}", sessionId);
        }
        remove(sessionId);

        SseEmitter emitter = new SseEmitter(timeout);
        emitters.put(sessionId, emitter);
        log.debug("새 SSE 연결 추가: {}, 총 연결 수: {}", sessionId, emitters.size());

        emitter.onCompletion(() -> {
            log.debug("SSE 연결 종료: {}", sessionId);
            remove(sessionId);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE 연결 타임아웃: {}", sessionId);
            remove(sessionId);
        });

        emitter.onError(e -> {
            log.warn("SSE 연결 에러: {}, 에러: {}", sessionId, e.getMessage());
            remove(sessionId);
        });

        return emitter;
    }

    public void remove(String sessionId) {
        SseEmitter emitter = emitters.remove(sessionId);
        if (emitter != null) {
            try {
                emitter.complete();
                log.debug("SSE 연결 제거: {}, 남은 연결 수: {}", sessionId, emitters.size());
            } catch (Exception e) {
                log.warn("SSE 연결 종료 중 에러: {}", sessionId, e);

            }
        }
    }

    @Async
    public void sendToAll(String eventName, Object data) {
        if(emitters.isEmpty()) {
            return;
        }

        log.debug("모든 클라이언트에게 '{}' 이벤트 전송, 대상 연결 수: {}", eventName, emitters.size());

        emitters.forEach((sessionId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (Exception e) {
                log.warn("클라이언트({})에게 이벤트({}) 전송 실패: {}", sessionId, eventName, e.getMessage());
                remove(sessionId);
            }
        });
    }

    @Scheduled(fixedRate = 60000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        log.debug("하트비트 이벤트 전송, 대상 연결 수: {}", emitters.size());
        emitters.forEach((sessionId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("HEARTBEAT")
                        .data(""));
            } catch (Exception e) {
                log.debug("하트비트 전송 실패로 연결 제거: {}", sessionId);
                remove(sessionId);
            }
        });
    }
}