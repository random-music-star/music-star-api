package com.curioussong.alsongdalsong.room.event;

import com.curioussong.alsongdalsong.common.sse.SseEmitterManager;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.dto.RoomDTO;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RoomEventListener {

    private final RoomRepository roomRepository;
    private final SseEmitterManager sseEmitterManager;

    // 비동기로 분리하는게? 현재는 동기처리 방식
    @EventListener
    public void handleRoomUpdatedEvent(RoomUpdatedEvent event) {
        if (event.actionType() != RoomUpdatedEvent.ActionType.DELETED) {
            Room room = roomRepository.findById(event.roomId()).orElse(null);
            if (room != null) {
                Map<String, Object> data = new HashMap<>();
                RoomDTO roomDTO = room.toDto();
                roomDTO.setGameModes(List.of("FULL"));

                data.put("room", roomDTO);
                data.put("actionType", event.actionType().name());

                sseEmitterManager.sendToAll("lobby", "ROOM_UPDATED", data);
            }
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("roomId", event.roomId());
            data.put("actionType", "DELETED");

            sseEmitterManager.sendToAll("lobby", "ROOM_UPDATED", data);
        }
    }
}