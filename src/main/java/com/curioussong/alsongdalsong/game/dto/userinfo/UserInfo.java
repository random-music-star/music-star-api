package com.curioussong.alsongdalsong.game.dto.userinfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserInfo {
    private String userName;
    private Boolean isReady;
    private Boolean isHost;
    private String colorCode;
}
