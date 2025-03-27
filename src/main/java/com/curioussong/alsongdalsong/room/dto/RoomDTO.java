package com.curioussong.alsongdalsong.room.dto;

import com.curioussong.alsongdalsong.game.domain.GameMode;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RoomDTO {
    private String id;
    private String title;
    private String hostName;
    private String format;
    private Integer maxPlayer;
    private Integer currentPlayers;
    private Integer maxGameRound;
    private Integer playTime;
    private String status;
    private boolean hasPassword;
    private List<GameMode> gameModes;

    public RoomDTO setGameModes(List<GameMode> gameModes) {
        this.gameModes = gameModes;
        return this;
    }
}