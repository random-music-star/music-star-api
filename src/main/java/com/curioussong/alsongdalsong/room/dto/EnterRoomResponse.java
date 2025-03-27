package com.curioussong.alsongdalsong.room.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EnterRoomResponse {
    private String roomId;
    private boolean success;
}
