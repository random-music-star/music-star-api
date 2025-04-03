package com.curioussong.alsongdalsong.game.dto.hint;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class HintResponse {
    private String title;
    private String singer;
    private String title2;
    private String singer2;
}
