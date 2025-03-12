package com.curioussong.alsongdalsong.chat.service;

import com.curioussong.alsongdalsong.chat.dto.ChatRequest;
import com.curioussong.alsongdalsong.chat.dto.ChatResponse;
import com.curioussong.alsongdalsong.chat.dto.Response;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    public ChatResponse chatMessage(ChatRequest chatRequest) {
        return ChatResponse.builder()
                .type(chatRequest.getType())
                .response(Response.builder()
                        .sender(chatRequest.getRequest().getSender())
                        .message(chatRequest.getRequest().getMessage())
                        .messageType("default")
                        .build())
                .build();
    }
}
