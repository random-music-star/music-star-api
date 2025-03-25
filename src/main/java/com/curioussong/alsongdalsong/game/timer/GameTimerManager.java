package com.curioussong.alsongdalsong.game.timer;

import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import com.curioussong.alsongdalsong.util.KoreanConsonantExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameTimerManager {

    private final RoomManager roomManager;
    private final GameMessageSender gameMessageSender;

    // 타이머 관련 상수
    private final Integer CONSONANT_HINT_TIME = 15;
    private final Integer SINGER_HINT_TIME = 30;

    // 방별 타이머 스케줄러 관리
    private Map<Long, ScheduledExecutorService> roomConsonantHintTimerSchedulers = new ConcurrentHashMap<>();
    private Map<Long, ScheduledExecutorService> roomSingerHintTimerSchedulers = new ConcurrentHashMap<>();
    private Map<Long, ScheduledExecutorService> roomRoundTimerSchedulers = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledExecutorService> roomSchedulers = new ConcurrentHashMap<>();

    public void startCountdown(String destination, Long roomId, Runnable onCountdownComplete) {
        new Thread(() -> {
            try {
                for (int i = 3; i > 0; i--) {
                    gameMessageSender.sendCountdown(destination, i);
                    Thread.sleep(1000);
                }
                gameMessageSender.sendCountdown(destination, 0);
                onCountdownComplete.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Countdown interrupted: {}", e.getMessage());
            }
        }).start();
    }

    public void scheduleSongPlayTime(String destination, int waitTimeInSeconds, Long roomId, Runnable endRoundAction) {
        initializeSchedulers(roomId);

        // 자음 힌트 스케줄링
        roomConsonantHintTimerSchedulers.get(roomId).schedule(
                () -> sendConsonantHint(destination, roomId),
                CONSONANT_HINT_TIME,
                TimeUnit.SECONDS
        );

        // 가수 힌트 스케줄링
        roomSingerHintTimerSchedulers.get(roomId).schedule(
                () -> sendSingerHint(destination, roomId),
                SINGER_HINT_TIME,
                TimeUnit.SECONDS
        );

        // 라운드 종료 스케줄링
        roomRoundTimerSchedulers.get(roomId).schedule(
                endRoundAction,
                waitTimeInSeconds,
                TimeUnit.SECONDS
        );
    }

    public void initializeSchedulers(Long roomId) {
        roomConsonantHintTimerSchedulers.put(roomId, Executors.newSingleThreadScheduledExecutor());
        roomSingerHintTimerSchedulers.put(roomId, Executors.newSingleThreadScheduledExecutor());
        roomRoundTimerSchedulers.put(roomId, Executors.newSingleThreadScheduledExecutor());
    }

    private void sendConsonantHint(String destination, Long roomId) {
        String consonants = KoreanConsonantExtractor.extractConsonants(
                roomManager.getSong(roomId).getKorTitle()
        );
        gameMessageSender.sendHint(destination, consonants, null);
    }

    private void sendSingerHint(String destination, Long roomId) {
        String consonants = KoreanConsonantExtractor.extractConsonants(
                roomManager.getSong(roomId).getKorTitle()
        );
        gameMessageSender.sendHint(destination, consonants, roomManager.getSong(roomId).getArtist());
    }

    public void cancelHintTimers(Long roomId) {
        if (roomConsonantHintTimerSchedulers.containsKey(roomId)) {
            roomConsonantHintTimerSchedulers.get(roomId).shutdownNow();
        }

        if (roomSingerHintTimerSchedulers.containsKey(roomId)) {
            roomSingerHintTimerSchedulers.get(roomId).shutdownNow();
        }
    }

    public void cancelRoundTimerAndTriggerImmediately(Long roomId, Runnable onRoundEnd) {
        if (roomRoundTimerSchedulers.containsKey(roomId)) {
            roomRoundTimerSchedulers.get(roomId).shutdownNow();
        }
        onRoundEnd.run();
    }

    public void shutdownAllTimers(Long roomId) {
        cancelHintTimers(roomId);

        if (roomRoundTimerSchedulers.containsKey(roomId)) {
            roomRoundTimerSchedulers.get(roomId).shutdownNow();
        }
    }
}