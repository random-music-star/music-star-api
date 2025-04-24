package com.curioussong.alsongdalsong.common.error.handler;

import com.curioussong.alsongdalsong.common.error.dto.StompErrorResponse;
import com.curioussong.alsongdalsong.common.error.stomperror.StompException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@ControllerAdvice
public class StompExceptionHandler extends StompSubProtocolErrorHandler {

    @MessageExceptionHandler(StompException.class)
    @SendToUser("/queue/system")
    public StompErrorResponse handleChatException(StompException ex) {
        return new StompErrorResponse(ex.getStompError().getStatusCode(), ex.getStompError().getMessage());
    }
}
