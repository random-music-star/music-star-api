package com.curioussong.alsongdalsong.game.dto.roominfo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RoomInfoResponseDTO {
    private String type;
    private RoomInfoResponse response;
}
