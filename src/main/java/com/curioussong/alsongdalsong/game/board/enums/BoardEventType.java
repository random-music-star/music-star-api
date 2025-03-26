package com.curioussong.alsongdalsong.game.board.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum BoardEventType {

    PLUS(10, false, true),
    MINUS(10, false, true),
    PULL(10, true, false),
    NOTHING(10, false, true),
    BOMB(10, false, true),
    CLOVER(10, false, true),
    SWAP(15, true, false),
    WARP(15, false, true),
    MAGNET(10, true, false),;

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
