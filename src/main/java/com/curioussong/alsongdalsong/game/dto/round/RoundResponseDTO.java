package com.curioussong.alsongdalsong.game.dto.round;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RoundResponseDTO {
    private String type;
    private RoundResponse response;
}
