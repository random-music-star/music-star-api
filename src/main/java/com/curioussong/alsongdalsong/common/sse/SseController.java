package com.curioussong.alsongdalsong.common.sse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterManager emitterManager;

    @GetMapping(value = "/{channelId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectToLobby(@PathVariable Long channelId, HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        String clientIP = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        SseEmitter emitter = emitterManager.addToChannel(sessionId, channelId, 3600000L);

        log.info("채널 로비 SSE 연결 요청: channelId={}, sessionId={}, IP={}, userAgent={}", channelId, sessionId, clientIP, userAgent);

        try {
            log.info("CONNECT 이벤트 전송: sessionId={}", sessionId);

            emitter.send(SseEmitter.event()
                    .name("CONNECT")
                    .data("채널 " + channelId + " 로비 SSE에 연결되었습니다."));

        } catch (IOException e) {
            log.error("채널 로비 SSE 연결 중 오류 발생: channelId={}, sessionId={}, error={}", channelId, sessionId, e.getMessage());

            emitter.completeWithError(e);
        }

        return emitter;
    }

    @GetMapping(value = "/channels", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectToChannelSelector(HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        String clientIP = request.getRemoteAddr();

        SseEmitter emitter = emitterManager.addForChannelSelection(sessionId, 300000L);
        log.info("채널 선택 화면 SSE 연결 요청: sessionId={}, clientIP={}", sessionId, clientIP);

        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECT")
                    .data("채널 선택 화면에 연결되었습니다."));

        } catch (IOException e) {
            log.error("채널 선택 화면 SSE 연결 중 오류 발생: sessionId={}, error={}",
                    sessionId, e.getMessage());
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @PostMapping("/disconnect")
    public void disconnect(HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        emitterManager.cleanupChannelSession(sessionId);
        emitterManager.cleanupSelectorSession(sessionId);
        log.info("SSE 연결 명시적 종료 요청: sessionId={}", sessionId);
    }
}
