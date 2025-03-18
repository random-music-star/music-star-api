package com.curioussong.alsongdalsong.room.event;

import com.curioussong.alsongdalsong.room.domain.Room;

public record RoomUpdatedEvent(Room room, ActionType actionType) {
    public enum ActionType {
        CREATED, UPDATED, DELETED
    }

    public RoomUpdatedEvent(Room room) {
        this(room, ActionType.UPDATED);
    }
}