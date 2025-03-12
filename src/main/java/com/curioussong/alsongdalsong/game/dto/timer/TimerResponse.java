package com.curioussong.alsongdalsong.game.dto.timer;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TimerResponse {
    private String type;
    private Response response;
}
