package com.curioussong.alsongdalsong.game.dto.board;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventTriggerResponseDTO {
    private final String type = "eventTrigger";
    private EventTriggerResponse response;
}
