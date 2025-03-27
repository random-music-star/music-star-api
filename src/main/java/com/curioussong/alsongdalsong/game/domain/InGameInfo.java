package com.curioussong.alsongdalsong.game.domain;

import com.curioussong.alsongdalsong.song.domain.Song;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.util.Pair;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class InGameInfo {
    private int currentRound;
    private int skipCount;
    private boolean answered;
    private Map<String, Integer> score;
    private Map<String, String> roundWinner; // 각 방의 현재 정답자 확인용
    private Map<String, Integer> userMovement;
    private Map<Integer, Pair<GameMode, Song>> roundInfo;
    private Map<String, Boolean> isSongPlaying;
    private Map<Long, Boolean> memberSkipStatus;

    public InGameInfo() {
        this.currentRound = 1;
        this.skipCount = 0;
        this.answered = false;
        this.score = new ConcurrentHashMap<>();
        this.roundWinner = new ConcurrentHashMap<>();
        this.userMovement = new ConcurrentHashMap<>();
        this.roundInfo = new ConcurrentHashMap<>();
        this.isSongPlaying = new ConcurrentHashMap<>();
        this.memberSkipStatus = new ConcurrentHashMap<>();
    }
}
