package com.curioussong.alsongdalsong.game.dto.round;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Response {
    private String mode;
    private Integer round;
}
