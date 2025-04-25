package com.curioussong.alsongdalsong.member.dto;

import com.curioussong.alsongdalsong.member.domain.Member;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberStatusDTO {
    private final List<MemberStatus> userList;

    public static MemberStatusDTO from(List<Member> members) {
        List<MemberStatus> statusList = members.stream()
                .map(MemberStatus::from)
                .toList();

        return new MemberStatusDTO(statusList);
    }

    public static MemberStatusDTO from(Member member) {
        return new MemberStatusDTO(List.of(MemberStatus.from(member)));
    }

    @JsonIgnore
    public String getUsername() {
        if (userList == null || userList.size() != 1) {
            return null;
        }
        return userList.get(0).getUsername();
    }
}
