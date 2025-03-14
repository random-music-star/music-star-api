package com.curioussong.alsongdalsong.game.dto.hint;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class HintResponseDTO {
    private String type;
    private HintResponse response;
}
