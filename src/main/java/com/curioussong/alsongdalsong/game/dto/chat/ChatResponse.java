package com.curioussong.alsongdalsong.game.dto.chat;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ChatResponse {
    private String sender;
    private String messageType;
    private String message;
}
