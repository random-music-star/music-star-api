package com.curioussong.alsongdalsong.channel.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ChannelListResponse(List<ChannelResponse> channels) {
}
