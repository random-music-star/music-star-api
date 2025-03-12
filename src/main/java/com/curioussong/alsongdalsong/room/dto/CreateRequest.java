package com.curioussong.alsongdalsong.room.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CreateRequest {
    private String title;
    private String password;
    private String format;
    private List<String> gameModes;
}
