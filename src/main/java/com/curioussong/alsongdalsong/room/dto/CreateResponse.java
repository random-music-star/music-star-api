package com.curioussong.alsongdalsong.room.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateResponse {
    private Long channelId;
    private String roomId;
    private Long roomNumber;
}
