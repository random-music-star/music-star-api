package com.curioussong.alsongdalsong.common.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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

    @GetMapping(value = "/lobby", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectToLobby() {
        SseEmitter emitter = emitterManager.add("lobby", 3600000L);

        try {
            // List<RoomDTO> rooms = roomService.getAllRooms();
            // emitter.send(SseEmitter.event().name("ROOM_LIST").data(rooms));

            emitter.send(SseEmitter.event()
                    .name("CONNECT")
                    .data("로비 SSE에 연결되었습니다."));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }
}