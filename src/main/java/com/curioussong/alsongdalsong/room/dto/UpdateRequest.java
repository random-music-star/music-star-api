package com.curioussong.alsongdalsong.room.dto;

import com.curioussong.alsongdalsong.game.domain.GameMode;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UpdateRequest {
    private Long roomId;
    private String title;
    private String format;
    private String password;
    private List<GameMode> gameModes;
    private List<Integer> selectedYears;
}
