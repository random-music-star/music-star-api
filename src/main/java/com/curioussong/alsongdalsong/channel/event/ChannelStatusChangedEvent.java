package com.curioussong.alsongdalsong.channel.event;

import com.curioussong.alsongdalsong.channel.enums.ChannelEventType;

public record ChannelStatusChangedEvent(Long channelId, ChannelEventType eventType, int playerCount) {
}
