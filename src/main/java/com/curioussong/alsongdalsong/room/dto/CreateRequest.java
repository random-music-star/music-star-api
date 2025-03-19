package com.curioussong.alsongdalsong.room.dto;

import com.curioussong.alsongdalsong.game.domain.GameMode;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CreateRequest {
    private Long channelId;
    private String title;
    private String password;
    private String format;
    private List<GameMode> gameModes;
    private List<Integer> selectedYears;
}
