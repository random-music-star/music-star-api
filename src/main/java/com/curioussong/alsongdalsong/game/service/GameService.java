package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.game.dto.timer.Response;
import com.curioussong.alsongdalsong.game.dto.timer.TimerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final SimpMessagingTemplate messagingTemplate;

    public void startGame(String channelId, String roomId) {
        String destination = String.format("/topic/channel/%s/room/%s", channelId, roomId);
        startCountdown(destination);
    }

    public void startCountdown(String destination) {
        new Thread(() -> {
            try {
                for (int i = 3; i > 0; i--) {
                    sendCountdown(destination, i);
                    Thread.sleep(1000);
                }
                sendCountdown(destination, 0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void sendCountdown(String destination, int countdown) {
        TimerResponse timerResponse = TimerResponse.builder()
                .type("timer")
                .response(Response.builder()
                        .remainTime(countdown)
                        .build())
                .build();

        log.info("Sending countdown response: {}", timerResponse);
        messagingTemplate.convertAndSend(destination, timerResponse);
    }
}
