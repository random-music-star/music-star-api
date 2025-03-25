package com.curioussong.alsongdalsong.game.dto.board;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardEventResponseDTO {
    private final String type = "event";
    private BoardEventResponse response;
}
