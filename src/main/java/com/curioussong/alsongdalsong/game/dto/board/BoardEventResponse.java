package com.curioussong.alsongdalsong.game.dto.board;

import com.curioussong.alsongdalsong.game.board.enums.BoardEventType;
import lombok.Builder;

@Builder
public record BoardEventResponse(BoardEventType eventType, String trigger, String target) {}
