package com.curioussong.alsongdalsong.member.dto;

import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.room.domain.Room;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberStatusDTO {
    private String username;
    private Member.MemberType memberType;
    private Boolean inLobby;
    private Room.RoomStatus roomStatus;

    public static MemberStatusDTO from(Member member) {
        MemberStatusDTOBuilder memberStatusDTO = MemberStatusDTO.builder()
                .username(member.getUsername())
                .memberType(member.getType())
                .inLobby(member.getRoom()==null);

        if(member.getRoom()!=null){
            memberStatusDTO.roomStatus(member.getRoom().getStatus());
        }

        return memberStatusDTO.build();
    }
}
