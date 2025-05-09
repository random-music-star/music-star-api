//package com.curioussong.alsongdalsong.game.event;
//
//import com.curioussong.alsongdalsong.game.domain.GameChat;
//import com.curioussong.alsongdalsong.game.repository.GameChatRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.event.EventListener;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//
//@Component
//@RequiredArgsConstructor
//public class GameChatSaveEventListener {
//
//    private final GameChatRepository gameChatRepository;
//
//    @Async
//    @EventListener
//    public void handleGameChatSaveEvent(GameChatSaveEvent event) {
//        GameChat gameChat = GameChat.builder()
//                .memberId(event.memberId())
//                .gameSessionId(event.gameSessionId())
//                .gameRoundId(event.gameRoundId())
//                .message(event.message())
//                .chattedAt(LocalDateTime.now())
//                .build();
//
//        gameChatRepository.save(gameChat);
//    }
//}
