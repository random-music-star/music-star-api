package com.curioussong.alsongdalsong.common.sse;

import com.curioussong.alsongdalsong.room.dto.RoomDTO;
import com.curioussong.alsongdalsong.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterManager emitterManager;
    private final RoomService roomService;

    @GetMapping(value = "/lobby", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectToLobby() {
        SseEmitter emitter = emitterManager.add("lobby", 3600000L);

        try {
            Page<RoomDTO> roomPage = roomService.getRooms(0, 8);

            Map<String, Object> response = new HashMap<>();
            response.put("rooms", roomPage.getContent());
            response.put("totalPages", roomPage.getTotalPages());
            response.put("totalElements", roomPage.getTotalElements());
            response.put("currentPage", roomPage.getNumber());

            emitter.send(SseEmitter.event()
                    .name("CONNECT")
                    .data("로비 SSE에 연결되었습니다."));

            emitter.send(SseEmitter.event()
                    .name("ROOM_LIST")
                    .data(response));

        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }
}