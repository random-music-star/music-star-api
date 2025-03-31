package com.curioussong.alsongdalsong.channel.controller;

import com.curioussong.alsongdalsong.channel.dto.ChannelListResponse;
import com.curioussong.alsongdalsong.channel.dto.ChannelResponse;
import com.curioussong.alsongdalsong.channel.service.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/channel")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @GetMapping()
    public ResponseEntity<ChannelListResponse> getAllChannels() {
        List<ChannelResponse> channels = channelService.getAllChannels();

        ChannelListResponse response = ChannelListResponse.builder()
                .channels(channels)
                .build();

        return ResponseEntity.ok(response);
    }
}
