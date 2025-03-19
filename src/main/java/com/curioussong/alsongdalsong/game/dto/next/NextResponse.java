package com.curioussong.alsongdalsong.game.dto.next;

import lombok.Builder;

@Builder
public record NextResponse(String username, Integer totalMovement) {
}
