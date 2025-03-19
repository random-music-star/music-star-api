package com.curioussong.alsongdalsong.game.dto.move;

import lombok.Builder;

@Builder
public record MoveResponseDTO(String type, MoveResponse response) {
}
