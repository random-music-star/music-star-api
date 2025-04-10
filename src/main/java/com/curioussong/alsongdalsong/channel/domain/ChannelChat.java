package com.curioussong.alsongdalsong.channel.domain;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "channel_chat")
@Builder
public class ChannelChat {
    @Id
    private String id;
    private Long memberId;
    private Long channelId;
    private String message;
    private LocalDateTime chattedAt;
}
