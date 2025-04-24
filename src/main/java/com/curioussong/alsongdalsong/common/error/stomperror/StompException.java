package com.curioussong.alsongdalsong.common.error.stomperror;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class StompException extends RuntimeException {
    private final StompError stompError;

}
