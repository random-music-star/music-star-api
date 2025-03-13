package com.curioussong.alsongdalsong.game.dto.userinfo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Builder
public class UserInfoResponse {
    private List<UserInfo> userInfoList;
}
