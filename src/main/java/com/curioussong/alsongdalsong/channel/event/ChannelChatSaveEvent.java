package com.curioussong.alsongdalsong.channel.event;

import com.curioussong.alsongdalsong.channel.domain.Channel;
import com.curioussong.alsongdalsong.member.domain.Member;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record ChannelChatSaveEvent (Member member, Channel channel, String message, LocalDateTime timeStamp) {
}
