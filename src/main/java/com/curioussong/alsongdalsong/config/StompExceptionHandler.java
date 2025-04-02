package com.curioussong.alsongdalsong.config;

import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.security.Principal;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class StompExceptionHandler {

    private final SimpMessageSendingOperations messagingTemplate;
    private final GameMessageSender gameMessageSender;

    @MessageExceptionHandler(RoomFullException.class)
    public void exception(RoomFullException e, Principal principal) {
        gameMessageSender.sendRefuseMessage(principal.getName());
        log.error("RoomFullException", e);
    }


    @MessageExceptionHandler(Exception.class)
    public void exception(Exception e, Principal principal) {
        String userId = principal.getName();

        Map<String, String> message = Map.of("code", "000", "message", "예상치 못한 예외 발생");
        messagingTemplate.convertAndSendToUser(userId, "/user/queue/system", message);

        log.error("Exception", e);
    }

}
