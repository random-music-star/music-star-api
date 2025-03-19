package com.curioussong.alsongdalsong.game.dto.move;

import lombok.Builder;

@Builder
public record MoveResponse(String username, Integer position) {
}
