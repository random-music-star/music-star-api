package com.curioussong.alsongdalsong.game.dto.gameend;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class GameEndResponse {
    private List<String> winner;
}
