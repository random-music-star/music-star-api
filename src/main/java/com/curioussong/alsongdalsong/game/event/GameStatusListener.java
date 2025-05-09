package com.curioussong.alsongdalsong.game.event;

import com.curioussong.alsongdalsong.common.sse.SseEmitterManager;
import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.event.MemberLocationEvent;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.dto.RoomDTO;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import com.curioussong.alsongdalsong.roomgame.repository.RoomGameRepository;
import com.curioussong.alsongdalsong.roomyear.domain.RoomYear;
import com.curioussong.alsongdalsong.roomyear.repository.RoomYearRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
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
    private final RoomGameRepository roomGameRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomYearRepository roomYearRepository;

    @EventListener
    @Transactional
    public void handleGameStatusEvent(GameStatusEvent event) {
        log.info("GameStatusEvent 발생 - roomId: {}, status: {}", event.room().getId(), event.status());

        Room room = roomRepository.findById(event.room().getId())
                .orElseThrow(() -> new IllegalStateException("해당 방을 찾을 수 없습니다."));

        try {
            Room.RoomStatus newStatus = Room.RoomStatus.valueOf(event.status());
            room.updateStatus(newStatus);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 상태 값: {}", event.status());
            return;
        }

        sendRoomUpdateToClients(room);

        for (Member member : room.getMembers()) {
            eventPublisher.publishEvent(new MemberLocationEvent(room.getChannel().getId(), member));
        }
    }

    private void sendRoomUpdateToClients(Room room) {
        Map<String, Object> data = new HashMap<>();
        RoomDTO roomDTO = room.toDto();
        List<GameMode> gameModes = roomGameRepository.findGameModesByRoomId(roomDTO.getId());
        roomDTO.setGameModes(gameModes);
        List<Integer> years = roomYearRepository.findAllByRoom(room).stream()
                .map(RoomYear::getYear)
                .toList();
        roomDTO.setYears(years);
        data.put("room", roomDTO);
        data.put("actionType", "UPDATED");

        Long channelId = room.getChannel().getId();
        sseEmitterManager.sendToChannel(channelId, "ROOM_UPDATED", data);
    }
}
