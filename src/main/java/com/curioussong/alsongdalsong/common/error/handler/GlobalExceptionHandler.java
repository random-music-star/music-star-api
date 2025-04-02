package com.curioussong.alsongdalsong.common.error.handler;

import com.curioussong.alsongdalsong.common.error.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            HttpClientErrorException.class
    })
    public ResponseEntity<ErrorResponse> handleHttpClientErrorExceptions(HttpClientErrorException e) {
        return ResponseEntity
                .status(e.getStatusCode())
                .body(ErrorResponse.builder()
                        .message(e.getStatusText())
                        .build());
    }
}