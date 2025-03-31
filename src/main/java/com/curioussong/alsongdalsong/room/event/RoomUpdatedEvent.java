package com.curioussong.alsongdalsong.room.event;

import com.curioussong.alsongdalsong.room.domain.Room;

public record RoomUpdatedEvent(Room room, Long channelId, ActionType actionType) {
    public enum ActionType {
        CREATED, UPDATED, FINISHED
    }
}