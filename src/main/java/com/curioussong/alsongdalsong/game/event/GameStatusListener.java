package com.curioussong.alsongdalsong.game.event;

import com.curioussong.alsongdalsong.common.sse.SseEmitterManager;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.dto.RoomDTO;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GameStatusListener {
    private final RoomRepository roomRepository;
    private final SseEmitterManager sseEmitterManager;

    @EventListener
    @Transactional
    public void handleGameStatusEvent(GameStatusEvent event) {
        Room room = roomRepository.findById(event.roomId()).orElse(null);
        if (room == null) {
            return;
        }

        room.updateStatus(event.status());
        roomRepository.save(room);

        sendRoomUpdateToClients(room);
    }

    private void sendRoomUpdateToClients(Room room) {
        Map<String, Object> data = new HashMap<>();
        RoomDTO roomDTO = room.toDto();
        roomDTO.setGameModes(List.of("FULL"));

        data.put("room", roomDTO);
        data.put("actionType", "UPDATED");

        sseEmitterManager.sendToAll("lobby", "ROOM_UPDATED", data);
    }
}

