package com.curioussong.alsongdalsong.common.error.stomperror;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StompError {
    ROOM_FULL(400, "방의 최대 인원에 도달하여 입장할 수 없습니다."),

    DUPLICATE_NICKNAME(409, "이미 존재하는 닉네임입니다."),

    USER_NOT_FOUND(400, "사용자를 찾을 수 없습니다."),
    CHANNEL_NOT_FOUND(400, "채널을 찾을 수 없습니다."),
    ROOM_NOT_FOUND(400, "방을 찾을 수 없습니다."),
    GAME_SESSION_NOT_FOUND(400, "해당 Game Session을 찾을 수 없습니다."),
    GAME_ROUND_NOT_FOUND(400, "해당 Game Round를 찾을 수 없습니다."),

    UNAUTHORIZED(401, "권한이 없습니다."),
    INTERNAL_ERROR(500, "서버 내부 오류가 발생했습니다.");

    private final int statusCode;
    private final String message;
}
