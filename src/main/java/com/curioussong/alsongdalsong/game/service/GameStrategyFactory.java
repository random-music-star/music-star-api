package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.room.domain.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameStrategyFactory {

    private final GeneralGameService generalGameService;
    private final BoardGameService boardGameService;

    public GameStrategy getStrategy(Room.RoomFormat format) {
        return switch (format) {
            case GENERAL -> generalGameService;
            case BOARD -> boardGameService;
        };
    }
}

