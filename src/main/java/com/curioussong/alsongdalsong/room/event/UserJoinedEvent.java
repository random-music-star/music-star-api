package com.curioussong.alsongdalsong.room.event;

public record UserJoinedEvent (String roomId, String sessionId, String username){
}
