package com.curioussong.alsongdalsong.chat.dto;

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
