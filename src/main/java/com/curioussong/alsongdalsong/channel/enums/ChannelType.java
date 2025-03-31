package com.curioussong.alsongdalsong.channel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@Getter
@RequiredArgsConstructor
public enum ChannelType {

    Name(1L),
    Name2(2L),
    Name3(3L),
    Name4(4L),
    Name5(5L);

    private final Long id;

    public static ChannelType getById(Long channelId) {
        if(channelId == null) {
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "존재하지 않는 방 번호입니다."
            );
        };
        for (ChannelType type : values()) {
            if (type.getId().equals(channelId)) {
                return type;
            }
        }
        throw new HttpClientErrorException(
                HttpStatus.BAD_REQUEST,
                "잘못된 채널 ID입니다."
                );
    }
}
