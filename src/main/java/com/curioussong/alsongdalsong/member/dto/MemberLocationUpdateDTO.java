package com.curioussong.alsongdalsong.member.dto;

import com.curioussong.alsongdalsong.member.enums.MemberLocationType;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberLocationUpdateDTO {
    private String username;
    private MemberLocationType location;
}
