package com.curioussong.alsongdalsong.game.board.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.concurrent.ThreadLocalRandom;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum BoardEventType {

    PLUS(20, false),
    MINUS(20, false),
    PULL(20, true),
    NOTHING(0, false),
    BOMB(20, false),
    CLOVER(20, false),
    SWAP(0, true),
    WARP(0, false),
    MAGNET(0, true);

    private final int probability;
    private final boolean hasTarget;

    public boolean hasTarget() {
        return this.hasTarget;
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
