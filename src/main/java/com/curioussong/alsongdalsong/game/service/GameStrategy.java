package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.game.dto.chat.ChatRequestDTO;
import com.curioussong.alsongdalsong.room.domain.Room;

import java.util.List;

public interface GameStrategy {

    void startRound(Long channelId, Room room, String destination);
    void initializeRound(Room room);
    void incrementSkipCount(String roomId, Long channelId, String username);
    boolean checkAnswer(ChatRequestDTO chatRequestDTO, String roomId);
    void handleAnswer(String userName, Long channelId, String roomId);
    void triggerEndEvent(String destination, Long channelId, Room room);
    void endGame(Room room, String destination);
    void handleRoundStart(String destination, Long channelId, Room room);
    List<String> findWinnerByScore(String roomId);
}
