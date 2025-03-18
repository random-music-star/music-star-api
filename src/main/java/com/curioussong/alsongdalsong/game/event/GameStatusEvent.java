package com.curioussong.alsongdalsong.game.event;

import com.curioussong.alsongdalsong.room.domain.Room;

public record GameStatusEvent(Room room, String status) {
}
