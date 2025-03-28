package com.curioussong.alsongdalsong.common.sse;

import com.curioussong.alsongdalsong.room.dto.LobbyResponse;
import com.curioussong.alsongdalsong.room.service.RoomService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterManager emitterManager;
    private final RoomService roomService;

    @GetMapping(value = "/lobby", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectToLobby(HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        String clientIP = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        SseEmitter emitter = emitterManager.add(sessionId, 3600000L);

        log.info("SSE 연결 요청: sessionId={}, IP={}, userAgent={}", sessionId, clientIP, userAgent);

        try {
            log.info("CONNECT 이벤트 전송: sessionId={}", sessionId);

            emitter.send(SseEmitter.event()
                    .name("CONNECT")
                    .data("로비 SSE에 연결되었습니다."));

            log.info("방 데이터 전송 시작: sessionId={}", sessionId);
            sendRoomData(emitter);
            log.info("방 데이터 전송 완료: sessionId={}", sessionId);

        } catch (IOException e) {
            log.error("SSE 연결 중 오류 발생: sessionId={}, error={}", sessionId, e.getMessage());

            emitter.completeWithError(e);
        }

        return emitter;
    }


    public void sendRoomData(SseEmitter emitter) {
        try {
            LobbyResponse roomData = roomService.getRoomDataForLobby();
            emitter.send(SseEmitter.event()
                    .name("ROOM_LIST")
                    .data(roomData));
        } catch (Exception e) {
            sendErrorEvent(emitter, "방 목록을 불러오는 중 오류 발생: "+e.getMessage());
        }
    }

    private void sendErrorEvent(SseEmitter emitter, String errorMessage) {
        try {
            emitter.send(SseEmitter.event()
                    .name("ERROR")
                    .data(errorMessage));
        } catch (Exception e) {
            log.warn("에러 메시지 전송 실패: {}", errorMessage, e);
        }
    }
}
