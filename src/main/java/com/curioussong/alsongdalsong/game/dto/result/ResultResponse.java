package com.curioussong.alsongdalsong.game.dto.result;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResultResponse {
    private String winner;
    private String songTitle;
    private String singer;
    private Integer score;
}
