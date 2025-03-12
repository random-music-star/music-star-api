package com.curioussong.alsongdalsong.socket.dto.chat;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ChatResponse {
    private String type;
    private Response response;
}
