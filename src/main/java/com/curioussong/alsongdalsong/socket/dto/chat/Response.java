package com.curioussong.alsongdalsong.socket.dto.chat;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Response {
    private String sender;
    private String messageType;
    private String message;
}
