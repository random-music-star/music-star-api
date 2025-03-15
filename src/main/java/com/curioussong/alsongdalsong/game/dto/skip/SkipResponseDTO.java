package com.curioussong.alsongdalsong.game.dto.skip;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SkipResponseDTO {
    private String type;
    private SkipResponse response;
}

