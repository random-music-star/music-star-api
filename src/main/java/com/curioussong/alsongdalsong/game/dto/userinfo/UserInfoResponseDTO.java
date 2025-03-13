package com.curioussong.alsongdalsong.game.dto.userinfo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserInfoResponseDTO {
    private String type;
    private UserInfoResponse response;
}
