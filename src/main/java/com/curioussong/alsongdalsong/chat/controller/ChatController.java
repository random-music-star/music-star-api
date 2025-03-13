package com.curioussong.alsongdalsong.chat.controller;

import com.curioussong.alsongdalsong.chat.dto.ChatRequest;
import com.curioussong.alsongdalsong.chat.dto.ChatResponse;
import com.curioussong.alsongdalsong.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/channel/{channelId}")
    @SendTo("/topic/channel/{channelId}")
    public ChatResponse handleChannelChat(ChatRequest chatRequest, @DestinationVariable Long channelId) {
        return chatService.channelChatMessage(chatRequest);
    }

    @MessageMapping("/channel/{channelId}/room/{roomId}")
    @SendTo("/topic/channel/{channelId}/room/{roomId}")
    public ChatResponse handleRoomChat(ChatRequest chatRequest, @DestinationVariable Long channelId, @DestinationVariable Long roomId) {
        return chatService.roomChatMessage(chatRequest, channelId, roomId);
    }
}

