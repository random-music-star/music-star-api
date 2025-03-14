package com.curioussong.alsongdalsong.game.dto.next;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class NextResponseDTO {
    private String type;
    private String response;
}
