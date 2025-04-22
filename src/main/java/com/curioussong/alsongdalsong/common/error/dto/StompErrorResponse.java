package com.curioussong.alsongdalsong.common.error.dto;

import lombok.Getter;

@Getter
public class StompErrorResponse {
    private final String type = "error";
    private final StompErrorDetail response;

    public StompErrorResponse(int code, String message) {
        this.response = new StompErrorDetail(code, message);
    }

}

