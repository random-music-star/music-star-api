package com.curioussong.alsongdalsong.game.dto.roominfo;

import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.room.domain.Room.RoomFormat;
import com.curioussong.alsongdalsong.room.domain.Room.RoomStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Builder
public class RoomInfoResponse {
    private String roomTitle;
    private Integer maxPlayer;
    private Integer maxGameRound;
    private RoomFormat format;
    private RoomStatus status;
    private List<GameMode> mode;
    private List<Integer> selectedYear;
    private Boolean hasPassword;
}
