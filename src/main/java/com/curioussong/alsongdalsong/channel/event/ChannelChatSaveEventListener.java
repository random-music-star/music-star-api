//package com.curioussong.alsongdalsong.channel.event;
//
//import com.curioussong.alsongdalsong.channel.domain.ChannelChat;
//import com.curioussong.alsongdalsong.channel.repository.ChannelChatRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.event.EventListener;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class ChannelChatSaveEventListener {
//
//    private final ChannelChatRepository channelChatRepository;
//
//    @Async
//    @EventListener
//    public void handleChannelChatSaveEvent(ChannelChatSaveEvent event) {
//        ChannelChat channelChat = ChannelChat.builder()
//                .memberId(event.member().getId())
//                .channelId(event.channel().getId())
//                .message(event.message())
//                .chattedAt(event.timeStamp())
//                .build();
//
//        channelChatRepository.save(channelChat);
//    }
//}
