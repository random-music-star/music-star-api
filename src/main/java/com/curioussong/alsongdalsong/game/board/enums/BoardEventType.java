package com.curioussong.alsongdalsong.game.board.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum BoardEventType {

    PLUS(40, false, true),
    MINUS(15, false, true),
    NOTHING(25, false, true),

    BOMB(1, false, true),
    CLOVER(1, false, true),

    PULL(5, true, false),
    SWAP(5, true, false),
    MAGNET(5, true, false),
    WARP(3, false, true),

    OVERLAP(0, true, false);

    private final int probability;
    private final boolean hasTarget;
    private final boolean isSoloPlayable;

    public boolean hasTarget() {
        return this.hasTarget;
    }

    public static BoardEventType getRandomSoloEventType() {
        // 솔로 플레이 가능한 이벤트들만 필터링
        BoardEventType[] soloPlayableEvents = Arrays.stream(values())
                .filter(BoardEventType::isSoloPlayable)
                .toArray(BoardEventType[]::new);

        // 솔로 플레이 가능한 이벤트들의 총 확률 합계 계산
        int totalProbability = Arrays.stream(soloPlayableEvents)
                .mapToInt(BoardEventType::getProbability)
                .sum();

        // 무작위 값 생성 (0 ~ totalProbability-1 사이)
        return getRandomEventTypeWithProbability(soloPlayableEvents, totalProbability);
    }

    public static BoardEventType getRandomEventType() {
        return getRandomEventTypeWithProbability(BoardEventType.values(), 100);
    }

    private static BoardEventType getRandomEventTypeWithProbability(BoardEventType[] eventTypes, int maxProbability) {
        if (maxProbability <= 0) {
            return BoardEventType.NOTHING;
        }

        int randomValue = ThreadLocalRandom.current().nextInt(maxProbability);

        int accumulatedProbability = 0;
        for (BoardEventType eventType : eventTypes) {
            accumulatedProbability += eventType.getProbability();
            if (randomValue < accumulatedProbability) {
                return eventType;
            }
        }
        return BoardEventType.NOTHING;
    }
}
