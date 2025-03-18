package com.curioussong.alsongdalsong.game.domain;

import com.curioussong.alsongdalsong.song.domain.Song;
import com.curioussong.alsongdalsong.song.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RoomManager {

    private final Map<Long, RoomInfo> roomMap = new ConcurrentHashMap<>();

    private final SongService songService;

    // 방 정보 반환
    public RoomInfo getRoomInfo(Long roomId) {
        return roomMap.get(roomId);
    }

    // 방의 현재 라운드 반환
    public int getCurrentRound(Long roomId) {
        return getRoomInfo(roomId).getCurrentRound();
    }

    // 방의 노래년도 리스트 반환
    public List<Integer> getSelectedYears(Long roomId) {
        return getRoomInfo(roomId).getSelectedYears();
    }

    // 방의 멤버 상태 초기화 (skip, ready → false)
    public void initializeMemberStatus(Long roomId) {
        RoomInfo roomInfo = getRoomInfo(roomId);
        Map<Long, Boolean> skipMember = roomInfo.getMemberSkipStatus();
        Map<Long, Boolean> readyMember = roomInfo.getMemberReadyStatus();

        for (Long memberId : readyMember.keySet()) {
            // skip 상태, ready 상태 false로 초기화
            skipMember.put(memberId, false);
            readyMember.put(memberId, false);
        }
    }

    // 방의 멤버 상태 초기화 (skip → false)
    public void initializeSkipStatus(Long roomId) {
        RoomInfo roomInfo = getRoomInfo(roomId);
        Map<Long, Boolean> memberToInfo = roomInfo.getMemberSkipStatus();

        for (Long memberId : memberToInfo.keySet()) {
            memberToInfo.put(memberId, false);
        }

        // skipCount 초기화
        roomInfo.setSkipCount(0);
    }

    // 방 레디 체크
    public boolean areAllPlayersReady(Long roomId) {
        RoomInfo roomInfo = getRoomInfo(roomId);
        Map<Long, Boolean> memberToInfo = roomInfo.getMemberReadyStatus();

        for (Boolean isReady : memberToInfo.values()) {
            if (!isReady) {
                return false; // 하나라도 준비되지 않았다면 false 반환
            }
        }

        return true;
    }

    // 게임 방 Song, Mode 설정
    public void initializeGameSetting(Long roomId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        int maxRound = roomInfo.getMaxGameRound();
        List<Song> selectedSongs = songService.getRandomSongByYear(roomInfo.getSelectedYears(), maxRound);
        for (int i = 0; i < maxRound; i++) {
            GameMode gameMode = GameMode.FULL;  // 현재는 FULL 모드만 사용
            Song song = selectedSongs.get(i);

            // RoomInfo의 roundToInfo 맵에 게임 모드와 노래 저장
            roomInfo.getRoundToInfo().put(i, Pair.of(gameMode, song));
        }

        // 라운드 초기화
        roomInfo.setCurrentRound(1);
    }

    // 현재 라운드의 곡 정보 반환
    public Song getSong(Long roomId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        return roomInfo.getRoundToInfo().get(roomInfo.getCurrentRound()).getSecond();
    }

    // 정답 상태 초기화
    public void initAnswer(Long roomId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        roomInfo.setAnswered(false);
    }

    // 라운드 증가
    public void nextRound(Long roomId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        roomInfo.setCurrentRound(roomInfo.getCurrentRound() + 1);
    }

    // 해당 멤버의 ready 상태 반환
    public boolean getReady(Long roomId, Long memberId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        return roomInfo.getMemberReadyStatus().get(memberId);
    }

    // 현재 라운드 정답 여부
    public boolean isAnswered(Long roomId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        return roomInfo.isAnswered();
    }

    // 정답 여부 처리
    public void setAnswer(Long roomId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        roomInfo.setAnswered(true);
    }

    // 해당 멤버 스킵 여부
    public boolean isSkipped(Long roomId, Long memberId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        return roomInfo.getMemberSkipStatus().get(memberId);
    }

    // 해당 멤버 스킵 처리
    public void setSkip(Long roomId, Long memberId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        roomInfo.getMemberSkipStatus().put(memberId, true);
    }

    // 스킵 카운트 증가 후 카운트 반환
    public int raiseSkip(Long roomId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        roomInfo.setSkipCount(roomInfo.getSkipCount() + 1);
        return roomInfo.getSkipCount();
    }

    // Ready 상태 Map 반환
    public Map<Long, Boolean> getReadyStatus(Long roomId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        return roomInfo.getMemberReadyStatus();
    }
}
