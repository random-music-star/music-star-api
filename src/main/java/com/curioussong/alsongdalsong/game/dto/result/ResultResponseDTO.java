package com.curioussong.alsongdalsong.game.dto.result;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResultResponseDTO {
    private String type;
    private ResultResponse response;
}
