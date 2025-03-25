package com.curioussong.alsongdalsong.game.dto.eventtrigger;

import lombok.Builder;

@Builder
public record EventTriggerResponseDTO(String type, EventTriggerResponse response) {
}
