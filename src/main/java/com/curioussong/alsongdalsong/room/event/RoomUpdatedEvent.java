package com.curioussong.alsongdalsong.room.event;

public record RoomUpdatedEvent(Long roomId, ActionType actionType) {
    public enum ActionType {
        CREATED, UPDATED, DELETED
    }

    public RoomUpdatedEvent(Long roomId) {
        this(roomId, ActionType.UPDATED);
    }
}