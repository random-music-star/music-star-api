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
        if (gameService.checkAnswer(chatRequest, roomId)) {
            gameService.handleAnswer(chatRequest.getRequest().getSender(), channelId, roomId);
        }

        // Skip 요청 처리
        if (".".equals(chatRequest.getRequest().getMessage())) {
            gameService.incrementSkipCount(roomId, channelId);
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
