package com.curioussong.alsongdalsong.room.event;

public record UserLeavedEvent(String sessionId, Long roomId, String username) {
}
