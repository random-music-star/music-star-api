package com.curioussong.alsongdalsong.room.event;

import com.curioussong.alsongdalsong.common.sse.SseEmitterManager;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.dto.RoomDTO;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RoomEventListener {

    private final RoomRepository roomRepository;
    private final SseEmitterManager sseEmitterManager;

    @EventListener
    public void handleRoomUpdatedEvent(RoomUpdatedEvent event) {
        List<Room> rooms = roomRepository.findAll();

        List<RoomDTO> roomDtos = rooms.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        sseEmitterManager.sendToAll("lobby", "ROOM_LIST_UPDATED", roomDtos);
    }

    private RoomDTO convertToDto(Room room) {
        return RoomDTO.builder()
                .id(room.getId())
                .title(room.getTitle())
                .hostName(room.getHost().getUsername())
                .format(room.getFormat().name())
                .maxPlayer(room.getMaxPlayer())
                .currentPlayers(room.getMemberIds().size())
                .hasPassword(room.getPassword() != null && !room.getPassword().isEmpty())
                .status(room.getStatus().name())
                .build();
    }
}