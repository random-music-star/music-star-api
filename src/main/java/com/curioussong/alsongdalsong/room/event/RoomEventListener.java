package com.curioussong.alsongdalsong.room.event;

import com.curioussong.alsongdalsong.common.sse.SseEmitterManager;
import com.curioussong.alsongdalsong.room.dto.RoomDTO;
import com.curioussong.alsongdalsong.room.event.RoomUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomEventListener {

    private final SseEmitterManager sseEmitterManager;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRoomUpdatedEvent(RoomUpdatedEvent event) {
        log.info("RoomUpdatedEvent 발생 - roomId: {}, actionType: {}", event.room().getId(), event.actionType());

        Map<String, Object> data = new HashMap<>();
        data.put("actionType", event.actionType().name());

        if (event.actionType() != RoomUpdatedEvent.ActionType.DELETED) {
            RoomDTO roomDTO = event.room().toDto();
            roomDTO.setGameModes(List.of("FULL"));
            data.put("room", roomDTO);
        } else {
            data.put("roomId", event.room().getId());
        }

        sseEmitterManager.sendToAll("ROOM_UPDATED", data);
    }
}
