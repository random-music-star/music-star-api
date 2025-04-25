package com.curioussong.alsongdalsong.member.dto;

import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.room.domain.Room;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberStatus {
    private final String username;
    private final Member.MemberType memberType;
    private final Boolean inLobby;
    private final Room.RoomStatus roomStatus;

    public static MemberStatus from(Member member) {
        return MemberStatus.builder()
                .username(member.getUsername())
                .memberType(member.getType())
                .inLobby(member.getRoom() == null)
                .roomStatus(member.getRoom() != null ? member.getRoom().getStatus() : null)
                .build();
    }
}
