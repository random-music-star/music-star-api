package com.curioussong.alsongdalsong.common.error.handler;

import com.curioussong.alsongdalsong.common.error.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = e.getMessage();

        if (message == null) {
            message = "입력 처리 중 오류가 발생했습니다.";
        }
        // GameMode Enum 변환 오류 처리
        else if (message.contains("Cannot deserialize value of type") && message.contains("GameMode")) {
            message = "지원하지 않는 게임모드가 포함되어 있습니다.";
        }
        // 기타 역직렬화 오류 처리
        else if (message.contains("Cannot deserialize value of type")) {
            message = "입력 형식이 올바르지 않습니다.";
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .message(message)
                        .build());
    }
}