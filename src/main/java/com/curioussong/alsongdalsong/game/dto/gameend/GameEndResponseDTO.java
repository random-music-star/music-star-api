package com.curioussong.alsongdalsong.game.dto.gameend;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GameEndResponseDTO {
    private String type;
    private GameEndResponse response;
}
