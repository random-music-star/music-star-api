package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.common.error.stomperror.StompError;
import com.curioussong.alsongdalsong.common.error.stomperror.StompException;
import com.curioussong.alsongdalsong.common.util.BadWordFilter;
import com.curioussong.alsongdalsong.common.util.Destination;
import com.curioussong.alsongdalsong.game.dto.chat.ChatRequestDTO;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameChatHandler {

    private final RoomRepository roomRepository;
    private final GameStrategyFactory gameStrategyFactory;
    private final GameMessageSender gameMessageSender;

    public void handleChat(ChatRequestDTO chatRequestDTO, Long channelId, String roomId) {
        String destination = Destination.room(channelId, roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new StompException(StompError.ROOM_NOT_FOUND));
        Room.RoomStatus roomStatus = room.getStatus();

        // 대기 중인 방에서는 채팅 메시지 필터링 수행
        if (roomStatus == Room.RoomStatus.WAITING) {
            filterMessage(chatRequestDTO);
            gameMessageSender.sendChat(chatRequestDTO, destination);
        } else if (roomStatus == Room.RoomStatus.IN_PROGRESS) {
            // 사용자들에게 채팅 메시지 전송
            gameMessageSender.sendChat(chatRequestDTO, destination);

            String message = chatRequestDTO.getRequest().getMessage();
            String sender = chatRequestDTO.getRequest().getSender();
            Room.RoomFormat roomFormat = room.getFormat();

            // 맵에 따라 다르게 처리
            GameStrategy strategy = gameStrategyFactory.getStrategy(roomFormat);
            if (isSkipChat(message)) {
                strategy.incrementSkipCount(roomId, channelId, sender);
            } else if (strategy.checkAnswer(chatRequestDTO, roomId)) {
                strategy.handleAnswer(sender, channelId, roomId);
            }
        }

        // 추후 mongodb 연결 시 주석 해제
//        if (room.getStatus() == Room.RoomStatus.IN_PROGRESS) {
//            saveInGameChat(chatRequestDTO, roomId);
//        }
    }

    private void filterMessage(ChatRequestDTO chatRequestDTO) {
        String filteredMessage = BadWordFilter.filter(chatRequestDTO.getRequest().getMessage());
        chatRequestDTO.getRequest().setMessage(filteredMessage);
    }

    private boolean isSkipChat(String message) {
        return ".".equals(message);
    }
}
