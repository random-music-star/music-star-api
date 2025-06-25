package com.curioussong.alsongdalsong.room.event;

import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.event.MemberLocationEvent;
import com.curioussong.alsongdalsong.room.domain.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomEventNotifier {
    private final ApplicationEventPublisher eventPublisher;

    public void notifyRommEvent(Room room, Long channelId, RoomUpdatedEvent.ActionType type) {
        eventPublisher.publishEvent(new RoomUpdatedEvent(room, channelId, type));
    }

    public void notifyUserJoined(String roomId, String sessionId, String username) {
        eventPublisher.publishEvent(new UserJoinedEvent(roomId, sessionId, username));
    }

    public void notifyMemberLocation(Long channelId, Member member) {
        eventPublisher.publishEvent(new MemberLocationEvent(channelId, member));
    }
}
