package com.curioussong.alsongdalsong.channel.dto;

import lombok.Builder;

@Builder
public record ChannelResponse(Long channelId, String name, Integer playerCount, Integer maxPlayers) {}
