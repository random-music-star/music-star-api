package com.curioussong.alsongdalsong.game.controller;

import com.curioussong.alsongdalsong.game.dto.start.StartRequest;
import com.curioussong.alsongdalsong.game.dto.userinfo.ReadyRequest;
import com.curioussong.alsongdalsong.game.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final GameService gameService;

    @MessageMapping("/channel/{channelId}/room/{roomId}/start")
    @SendTo("/topic/channel/{channelId}/room/{roomId}")
    public void handleRequest(StartRequest request, @DestinationVariable Long channelId, @DestinationVariable Long roomId) {
        if (request.getType().equals("gameStart")) {
            gameService.startGame(channelId, roomId);
        }
    }

    @MessageMapping("/channel/{channelId}/room/{roomId}/ready")
    @SendTo("/topic/channel/{channelId}/room/{roomId}")
    public void toggleReady(ReadyRequest request, @DestinationVariable Long channelId, @DestinationVariable Long roomId) {
        gameService.toggleReady(request.getUsername(), channelId, roomId);
    }
}
