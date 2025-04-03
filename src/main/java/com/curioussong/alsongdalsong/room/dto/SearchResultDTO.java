package com.curioussong.alsongdalsong.room.dto;

import com.curioussong.alsongdalsong.room.domain.Room;

public record SearchResultDTO(Room.RoomFormat format, String roomId, Long roomNumber, String title, Integer playerCount, Integer maxPlayer, Boolean hasPassword) {
}
