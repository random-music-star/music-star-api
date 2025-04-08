package com.curioussong.alsongdalsong.game.timer;

import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.game.domain.InGameManager;
import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.dto.song.SongInfo;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import com.curioussong.alsongdalsong.song.domain.Song;
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
    private final InGameManager inGameManager;

    // 방별 타이머 스케줄러 관리
    private Map<String, ScheduledExecutorService> roomConsonantHintTimerSchedulers = new ConcurrentHashMap<>();
    private Map<String, ScheduledExecutorService> roomSingerHintTimerSchedulers = new ConcurrentHashMap<>();
    private Map<String, ScheduledExecutorService> roomRoundTimerSchedulers = new ConcurrentHashMap<>();
    private final Map<String, ScheduledExecutorService> roomSchedulers = new ConcurrentHashMap<>();

    public void startCountdown(String destination, String roomId, Runnable onCountdownComplete) {
        new Thread(() -> {
            try {
                for (int i = 3; i > 0; i--) {
                    gameMessageSender.sendCountdown(destination, i);
                    Thread.sleep(2500);
                }
                gameMessageSender.sendCountdown(destination, 0);
                onCountdownComplete.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Countdown interrupted: {}", e.getMessage());
            }
        }).start();
    }

    public void handleRoundStart(String destination, int currentRound, GameMode gameMode,SongInfo currentRoundFirstSong, SongInfo currentRoundSecondSong, String roomId, Runnable roundStartComplete) {
        gameMessageSender.sendRoundInfo(destination, currentRound, gameMode, currentRoundFirstSong, currentRoundSecondSong);
        // 2.5초 후 roundInfo 전달
        roomRoundTimerSchedulers.get(roomId).schedule(
                () -> gameMessageSender.sendRoundOpen(destination),
                2500,
                TimeUnit.MILLISECONDS
        );
        // roundOpen 전달 1.5초 후 roundStart 전달
        roomRoundTimerSchedulers.get(roomId).schedule(
                () -> {
                    gameMessageSender.sendRoundStart(destination);
                    roundStartComplete.run();
                },
                4000,
                TimeUnit.MILLISECONDS
        );
    }

    public void scheduleSongPlayTime(String destination, int waitTimeInSeconds, String roomId, Runnable endRoundAction) {
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

    public void initializeSchedulers(String roomId) {
        roomConsonantHintTimerSchedulers.put(roomId, Executors.newSingleThreadScheduledExecutor());
        roomSingerHintTimerSchedulers.put(roomId, Executors.newSingleThreadScheduledExecutor());
        roomRoundTimerSchedulers.put(roomId, Executors.newSingleThreadScheduledExecutor());
    }

    private void sendConsonantHint(String destination, String roomId) {
        SongInfo firstSong = inGameManager.getCurrentRoundSong(roomId);
        String firstSongConsonants = KoreanConsonantExtractor.extractConsonants(firstSong.getKorTitle());
        if(inGameManager.hasSecondSongInCurrentRound(roomId)) {
            SongInfo secondSong = inGameManager.getSecondSongForCurrentRound(roomId);
            String secondSongConsonants = KoreanConsonantExtractor.extractConsonants(secondSong.getKorTitle());
            gameMessageSender.sendHint(destination, firstSongConsonants, null, secondSongConsonants, null);
        } else{
            gameMessageSender.sendHint(destination, firstSongConsonants, null, null, null);
        }
    }

    private void sendSingerHint(String destination, String roomId) {
        SongInfo firstSong = inGameManager.getCurrentRoundSong(roomId);
        String firstSongConsonants = KoreanConsonantExtractor.extractConsonants(firstSong.getKorTitle());
        if(inGameManager.hasSecondSongInCurrentRound(roomId)) {
            SongInfo secondSong = inGameManager.getSecondSongForCurrentRound(roomId);
            String secondSongConsonants = KoreanConsonantExtractor.extractConsonants(secondSong.getKorTitle());
            gameMessageSender.sendHint(destination, firstSongConsonants, firstSong.getArtist(), secondSongConsonants, secondSong.getArtist());
        } else{
            gameMessageSender.sendHint(destination, firstSongConsonants, firstSong.getArtist(), null, null);
        }
    }

    public void cancelHintTimers(String roomId) {
        if (roomConsonantHintTimerSchedulers.containsKey(roomId)) {
            roomConsonantHintTimerSchedulers.get(roomId).shutdownNow();
        }

        if (roomSingerHintTimerSchedulers.containsKey(roomId)) {
            roomSingerHintTimerSchedulers.get(roomId).shutdownNow();
        }
    }

    public void cancelRoundTimerAndTriggerImmediately(String roomId, Runnable onRoundEnd) {
        if (roomRoundTimerSchedulers.containsKey(roomId)) {
            roomRoundTimerSchedulers.get(roomId).shutdownNow();
        }
        onRoundEnd.run();
    }

    public void shutdownAllTimers(String roomId) {
        cancelHintTimers(roomId);

        if (roomRoundTimerSchedulers.containsKey(roomId)) {
            roomRoundTimerSchedulers.get(roomId).shutdownNow();
        }
    }
}