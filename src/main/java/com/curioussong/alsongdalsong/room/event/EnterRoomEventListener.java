package com.curioussong.alsongdalsong.room.event;

import com.curioussong.alsongdalsong.config.RoomFullException;
import com.curioussong.alsongdalsong.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnterRoomEventListener {

    private final RoomService roomService;

    @EventListener
    public void handleRoomEnterEvent(EnterRoomEvent event) {
        if (roomService.isRoomFull(event.roomId()) || roomService.isRoomInProgress(event.roomId()) || roomService.isRoomFinished(event.roomId())) {
            throw new RoomFullException("입장할 수 없는 방입니다.");
        }
    }
}
