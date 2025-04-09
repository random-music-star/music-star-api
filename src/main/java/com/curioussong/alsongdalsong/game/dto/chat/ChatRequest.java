package com.curioussong.alsongdalsong.game.dto.chat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequest {
    private String sender;
    private String message;
}
