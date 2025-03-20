package com.curioussong.alsongdalsong.room.event;

public record UserJoinedEvent (Long roomId, String sessionId, String username){
}
