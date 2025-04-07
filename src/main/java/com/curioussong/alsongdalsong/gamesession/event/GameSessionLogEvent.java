package com.curioussong.alsongdalsong.gamesession.event;

import com.curioussong.alsongdalsong.room.domain.Room;

public record GameSessionLogEvent(Room room, Type type) {
    public enum Type {
        START, END
    }
}