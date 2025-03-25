package com.curioussong.alsongdalsong.game.dto.board;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventEndResponseDTO {
    private final String type = "eventEnd";
    private final Object response = null;
}
