package com.curioussong.alsongdalsong.socket.service;

import com.curioussong.alsongdalsong.socket.dto.chat.ChatRequest;
import com.curioussong.alsongdalsong.socket.dto.chat.ChatResponse;
import com.curioussong.alsongdalsong.socket.dto.chat.Response;
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
