package com.curioussong.alsongdalsong.game.domain;

import com.curioussong.alsongdalsong.song.domain.Song;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class RoomInfo {
    private Long roomId;
    private Long channelId;
    private int currentRound;
    private int maxGameRound;
    private int skipCount;
    private boolean answered;
    private List<Integer> selectedYears;
    private Map<String, Integer> score;
    private Map<Long, String> roundWinner; // 각 방의 현재 정답자 확인용

    private Map<Integer, Pair<GameMode, Song>> roundToInfo;

    // memberId에 따른 skip상태, ready상태
    private Map<Long, Boolean> memberReadyStatus;
    private Map<Long, Boolean> memberSkipStatus;

    public RoomInfo(Long roomId, Long channelId) {
        this.roomId = roomId;
        this.channelId = channelId;
        this.currentRound = 1;
        this.maxGameRound = 20; // TODO : 임의로 설정
        this.skipCount = 0;
        this.answered = false;
        this.selectedYears = new ArrayList<>();
        this.roundToInfo = new ConcurrentHashMap<>();
        this.memberReadyStatus = new ConcurrentHashMap<>();
        this.memberSkipStatus = new ConcurrentHashMap<>();
        this.score = new ConcurrentHashMap<>();
        this.roundWinner = new ConcurrentHashMap<>();
    }

}
