package com.curioussong.alsongdalsong.channel.event;

import com.curioussong.alsongdalsong.channel.service.ChannelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelEventListener {

    private final ChannelService channelService;

    @Async
    @EventListener
    public void handleChannelStatusChanged(ChannelStatusChangedEvent event) {
        log.debug("[Event] 채널 상태 변경 이벤트 수신: channelId={}, eventType={}, playerCount={}", event.channelId(), event.eventType(), event.playerCount());
        channelService.notifyChannelUpdate(event.channelId());
    }
}
