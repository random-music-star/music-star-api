package com.curioussong.alsongdalsong.game.dto.test;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TestResponseDTO {
    private String type;
    private TestResponse response;
}
