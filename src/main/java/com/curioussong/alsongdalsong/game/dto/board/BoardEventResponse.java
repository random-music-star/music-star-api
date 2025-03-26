package com.curioussong.alsongdalsong.game.dto.board;

import com.curioussong.alsongdalsong.game.board.enums.BoardEventType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardEventResponse {
    private BoardEventType eventType;
    private String trigger;
    private String target;

    public void updateEventType(BoardEventType eventType) {
        this.eventType = eventType;
    }

    public void updateTarget(String target) {
        this.target = target;
    }
}
