package com.curioussong.alsongdalsong.game.dto.chat;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ChatResponseDTO {
    private String type;
    private ChatResponse response;
}
