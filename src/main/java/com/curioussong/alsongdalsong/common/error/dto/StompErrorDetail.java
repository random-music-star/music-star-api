package com.curioussong.alsongdalsong.common.error.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StompErrorDetail {
    private final int statusCode;
    private final String message;

}