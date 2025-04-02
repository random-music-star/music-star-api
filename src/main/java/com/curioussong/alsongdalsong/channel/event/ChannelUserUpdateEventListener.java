package com.curioussong.alsongdalsong.channel.event;

import com.curioussong.alsongdalsong.channel.enums.ChannelEventType;
import com.curioussong.alsongdalsong.common.sse.SseEmitterManager;
import com.curioussong.alsongdalsong.member.dto.MemberStatusDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelUserUpdateEventListener {

    private final SseEmitterManager sseEmitterManager;

    @Async
    @EventListener
    public void handleChannelUserUpdateEvent(ChannelUserUpdateEvent event) {
        Long channelId = event.channelId();
        MemberStatusDTO memberStatus = event.memberStatus();
        ChannelEventType eventType = event.eventType();

        Map<String, Object> userData = new HashMap<>();

        if(eventType == ChannelEventType.JOIN){
            userData.put("member", memberStatus);
            sseEmitterManager.sendToChannel(channelId, "USER_JOINED_CHANNEL", userData);
        } else if(eventType == ChannelEventType.LEAVE){
            userData.put("username", memberStatus.getUsername());
            sseEmitterManager.sendToChannel(channelId, "USER_LEFT_CHANNEL", userData);
        }
    }
}
