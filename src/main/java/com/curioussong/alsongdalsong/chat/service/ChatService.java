package com.curioussong.alsongdalsong.chat.service;

import com.curioussong.alsongdalsong.chat.dto.ChatRequest;
import com.curioussong.alsongdalsong.chat.dto.ChatResponse;
import com.curioussong.alsongdalsong.chat.dto.Response;
import com.curioussong.alsongdalsong.game.service.GameService;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final GameService gameService;

    public ChatService(GameService gameService) {
        this.gameService = gameService;
    }

    public ChatResponse channelChatMessage(ChatRequest chatRequest) {
        return ChatResponse.builder()
                .type(chatRequest.getType())
                .response(Response.builder()
                        .sender(chatRequest.getRequest().getSender())
                        .message(chatRequest.getRequest().getMessage())
                        .messageType("default")
                        .build())
                .build();
    }

    public ChatResponse roomChatMessage(ChatRequest chatRequest, Long channelId, Long roomId) {
        String userName = chatRequest.getRequest().getSender();
        String message = chatRequest.getRequest().getMessage();
        if (message.equals("톰보이")) {
            gameService.answer(userName, channelId, roomId);
        }
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
