package com.curioussong.alsongdalsong.game.board.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.concurrent.ThreadLocalRandom;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum BoardEventType {

    PLUS(15, false),
    MINUS(15, false),
    PULL(10, true),
    NOTHING(10, false),
    BOMB(10, false),
    OVERLAP(0, true),
    CLOVER(10, false),
    SWAP(10, true),
    WARP(10, false),
    MAGNET(10, true);

    private final int probability;
    private final boolean hasTarget;

    public static boolean getHasTarget(BoardEventType eventType) {
        return eventType.hasTarget;
    }

    public static BoardEventType getRandomEventType() {
        int randomValue = ThreadLocalRandom.current().nextInt(100);

        int accumulatedProbability = 0;
        for (BoardEventType eventType : BoardEventType.values()) {
            accumulatedProbability += eventType.probability;
            if (randomValue < accumulatedProbability) {
                return eventType;
            }
        }
        return BoardEventType.NOTHING;
    }

}
