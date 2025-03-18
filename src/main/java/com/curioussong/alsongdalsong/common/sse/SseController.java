package com.curioussong.alsongdalsong.common.sse;

import com.curioussong.alsongdalsong.room.dto.LobbyResponse;
import com.curioussong.alsongdalsong.room.service.RoomService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterManager emitterManager;
    private final RoomService roomService;

    @GetMapping(value = "/lobby", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectToLobby(HttpServletRequest request) {
        String sessionId = request.getSession().getId();
//        SseEmitter emitter = emitterManager.add(sessionId, 3600000L);

        SseEmitter emitter = new SseEmitter(3600000L);
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECT")
                    .data("로비 SSE에 연결되었습니다."));

            sendRoomData(emitter);
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }


    @Async
    public void sendRoomData(SseEmitter emitter) {
        try {
            LobbyResponse roomData = roomService.getRoomDataForLobby();
            emitter.send(SseEmitter.event()
                    .name("ROOM_LIST")
                    .data(roomData));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
}
