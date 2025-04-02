package com.curioussong.alsongdalsong.member.event;

import com.curioussong.alsongdalsong.common.sse.SseEmitterManager;
import com.curioussong.alsongdalsong.member.dto.MemberLocationUpdateDTO;
import com.curioussong.alsongdalsong.member.enums.MemberLocationType;
import com.curioussong.alsongdalsong.room.domain.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberLocationEventListener {

    private final SseEmitterManager sseEmitterManager;

    @EventListener
    public void handleMemberLocationEvent(MemberLocationEvent event) {
        log.info("handleMemberLocationEvent: {}", event);
        Long channelId = event.channelId();
        MemberLocationType location;
        Room room = event.member().getRoom();

        if(room==null){
            location = MemberLocationType.LOBBY;
        } else {
            location = switch(room.getStatus()){
                case WAITING -> MemberLocationType.WAITING_ROOM;
                case IN_PROGRESS -> MemberLocationType.IN_PROGRESS_ROOM;
                default -> MemberLocationType.LOBBY;
            };
        }

        MemberLocationUpdateDTO userData = MemberLocationUpdateDTO.builder()
                .username(event.member().getUsername())
                .location(location)
                .build();

        sseEmitterManager.sendToChannel(channelId, "MEMBER_LOCATION_UPDATED", userData);
    }
}
