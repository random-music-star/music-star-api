package com.curioussong.alsongdalsong.socket.controller;

import com.curioussong.alsongdalsong.socket.dto.chat.ChatRequest;
import com.curioussong.alsongdalsong.socket.dto.chat.ChatResponse;
import com.curioussong.alsongdalsong.socket.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/channel/{channelId}")
    @SendTo("/topic/channel/{channelId}")
    public ChatResponse handleChannelChat(ChatRequest chatRequest) {
        return chatService.chatMessage(chatRequest);
    }
}

