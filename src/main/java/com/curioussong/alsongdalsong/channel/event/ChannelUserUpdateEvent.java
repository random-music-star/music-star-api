package com.curioussong.alsongdalsong.channel.event;

import com.curioussong.alsongdalsong.channel.enums.ChannelEventType;
import com.curioussong.alsongdalsong.member.dto.MemberStatusDTO;

public record ChannelUserUpdateEvent(Long channelId, MemberStatusDTO memberStatus, ChannelEventType eventType) {
}
