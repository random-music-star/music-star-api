package com.curioussong.alsongdalsong.room.event;

import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserJoinedEventListener {

    private final MemberService memberService;
    private final RoomManager roomManager;

    @EventListener
    public void handleUserJoinedEvent(UserJoinedEvent event) {
        Member member = memberService.getMemberByToken(event.username());
        // host는 방을 만들면서 status가 초기화 되어 있음.
        // host 외에 다른 사람이 방에 입장할 때 status 설정 해줘야 함.
        if (roomManager.getRoomInfo(event.roomId()).getMemberReadyStatus().get(member.getId()) == null) {
            roomManager.getReadyStatus(event.roomId()).put(member.getId(), false);
            roomManager.getSkipStatus(event.roomId()).put(member.getId(), false);
        }
    }
}