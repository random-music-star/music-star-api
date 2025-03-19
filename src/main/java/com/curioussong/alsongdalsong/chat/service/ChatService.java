package com.curioussong.alsongdalsong.chat.service;

import com.curioussong.alsongdalsong.chat.dto.ChatRequest;
import com.curioussong.alsongdalsong.chat.dto.ChatResponse;
import com.curioussong.alsongdalsong.chat.dto.Response;
import com.curioussong.alsongdalsong.game.service.GameService;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final GameService gameService;
    private final RoomRepository roomRepository;

    public ChatService(GameService gameService, RoomRepository roomRepository) {
        this.gameService = gameService;
        this.roomRepository = roomRepository;
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
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));

        if (room.getStatus() == Room.RoomStatus.WAITING) {
            return ChatResponse.builder()
                    .type(chatRequest.getType())
                    .response(Response.builder()
                            .sender(chatRequest.getRequest().getSender())
                            .message(chatRequest.getRequest().getMessage())
                            .messageType("default")
                            .build())
                    .build();
        }

        if (gameService.checkAnswer(chatRequest, roomId)) {
            gameService.handleAnswer(chatRequest.getRequest().getSender(), channelId, roomId);
        }

        // Skip 요청 처리
        if (".".equals(chatRequest.getRequest().getMessage())) {
            gameService.incrementSkipCount(roomId, channelId, chatRequest.getRequest().getSender());
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
