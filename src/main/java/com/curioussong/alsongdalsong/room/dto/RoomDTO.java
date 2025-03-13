package com.curioussong.alsongdalsong.room.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomDTO {
    private Long id;
    private String title;
    private String hostName;
    private String format;
    private Integer maxPlayer;
    private Integer currentPlayers;
    private Integer maxGameRound;
    private Integer playTime;
    private String status;
    private boolean hasPassword;
    private String gameMode;
}