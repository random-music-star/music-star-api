package com.curioussong.alsongdalsong.room.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnterRoomRequest {
    private String roomId;
    private String password;
}
