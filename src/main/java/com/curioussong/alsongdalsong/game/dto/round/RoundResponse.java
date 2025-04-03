package com.curioussong.alsongdalsong.game.dto.round;

import com.curioussong.alsongdalsong.game.domain.GameMode;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RoundResponse {
    private GameMode mode;
    private Integer round;
    private String songUrl;
    private String songUrl2;
}
