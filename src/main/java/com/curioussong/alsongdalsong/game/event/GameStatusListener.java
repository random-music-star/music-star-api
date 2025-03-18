package com.curioussong.alsongdalsong.game.event;

import com.curioussong.alsongdalsong.common.sse.SseEmitterManager;
import com.curioussong.alsongdalsong.game.event.GameStatusEvent;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.dto.RoomDTO;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameStatusListener {

    private final RoomRepository roomRepository;
    private final SseEmitterManager sseEmitterManager;

    @Async // 🚀 비동기 처리 추가
    @EventListener
    @Transactional
    public void handleGameStatusEvent(GameStatusEvent event) {
        log.info("GameStatusEvent 발생 - roomId: {}, status: {}", event.room().getId(), event.status());

        Room room = roomRepository.findById(event.room().getId())
                .orElseThrow(() -> new IllegalStateException("해당 방을 찾을 수 없습니다."));

        try {
            Room.RoomStatus newStatus = Room.RoomStatus.valueOf(event.status());
            room.updateStatus(newStatus); // 🚀 JPA 영속성 컨텍스트가 변경 감지
        } catch (IllegalArgumentException e) {
            log.error("잘못된 상태 값: {}", event.status());
            return; // 잘못된 상태값이면 처리 중단
        }

        sendRoomUpdateToClients(room);
    }

    private void sendRoomUpdateToClients(Room room) {
        Map<String, Object> data = new HashMap<>();
        RoomDTO roomDTO = room.toDto();
        roomDTO.setGameModes(List.of("FULL"));

        data.put("room", roomDTO);
        data.put("actionType", "UPDATED");

        sseEmitterManager.sendToAll("ROOM_UPDATED", data);
    }
}
