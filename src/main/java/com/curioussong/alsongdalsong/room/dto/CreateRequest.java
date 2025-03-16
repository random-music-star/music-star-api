package com.curioussong.alsongdalsong.room.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CreateRequest {
    private String title;
    private String password;
    private String format;
    private List<String> gameModes;
    private List<Integer> selectedYears;
}
