package com.curioussong.alsongdalsong.game.dto.event;

import com.curioussong.alsongdalsong.game.board.enums.BoardEventType;

public record EventResponse(BoardEventType eventType, String trigger, String target) {
}
