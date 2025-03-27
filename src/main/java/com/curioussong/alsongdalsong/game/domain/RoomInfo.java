package com.curioussong.alsongdalsong.game.domain;

import com.curioussong.alsongdalsong.game.board.enums.BoardEventType;
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
    private String roomId;
    private Long channelId;
    private int maxGameRound;
    private List<Integer> selectedYears;
    private final Map<BoardEventType, Pair<String, Integer>> interactionEventTarget;

    // memberId에 따른 ready상태
    private Map<Long, Boolean> memberReadyStatus;

    public RoomInfo(String roomId, Long channelId) {
        this.roomId = roomId;
        this.channelId = channelId;
        this.maxGameRound = 20; // TODO : 임의로 설정
        this.selectedYears = new ArrayList<>();
        this.memberReadyStatus = new ConcurrentHashMap<>();
        this.interactionEventTarget = new ConcurrentHashMap<>();
    }

}
