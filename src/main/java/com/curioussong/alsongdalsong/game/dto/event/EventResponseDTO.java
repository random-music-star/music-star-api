package com.curioussong.alsongdalsong.game.dto.event;

import lombok.Builder;

@Builder
public record EventResponseDTO(String type, EventResponse response) {
}
