package com.curioussong.alsongdalsong.game.domain;

import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.roomgame.repository.RoomGameRepository;
import com.curioussong.alsongdalsong.song.domain.Song;
import com.curioussong.alsongdalsong.song.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class InGameManager {

    private final RoomGameRepository roomGameRepository;

    private final Map<Long, InGameInfo> inGameMap = new ConcurrentHashMap<>();
    private final SongService songService;
    private final RoomManager roomManager;

    public void initializeGameSettings(Room room) {
        InGameInfo inGameInfo = new InGameInfo();
        inGameMap.put(room.getId(), inGameInfo);
        inGameInfo.setCurrentRound(1); // 라운드 초기화
        initializeScores(inGameInfo, room); // 점수 초기화
        initializeRoundWinner(room); // 정답자 초기화
        initializeUserMovement(inGameInfo, room); // 사용자 별 움직이는거리 초기화
        initializeSongs(inGameInfo, room); // 노래 세팅
        initializeSkipStatus(room); // 스킵 상태 초기화
    }

    private void initializeScores(InGameInfo inGameInfo, Room room) {
        List<Member> members = room.getMembers();
        Map<String, Integer> userScore = inGameInfo.getScore();
        userScore.clear();
        for (Member member : members) {
            userScore.put(member.getUsername(), 0);
        }
    }

    private void initializeUserMovement(InGameInfo inGameInfo, Room room) {
        List<Member> members = room.getMembers();
        Map<String, Integer> userMovement = inGameInfo.getUserMovement();
        for (Member member : members) {
            userMovement.put(member.getUsername(), 0);
        }
    }

    private void initializeSongs(InGameInfo inGameInfo, Room room) {
        RoomInfo roomInfo = roomManager.getRoomInfo(room.getId());
        List<Song> selectedSongs = songService.getRandomSongByYear(roomInfo.getSelectedYears(), room.getMaxGameRound());
        List<GameMode> gameModes = roomGameRepository.findGameModesByRoomId(room.getId());
        Random random = new Random();

        for (int i = 0; i < room.getMaxGameRound(); i++) {
            int randomIndex = random.nextInt(gameModes.size());
            GameMode gameMode = gameModes.get(randomIndex);
            Song song = selectedSongs.get(i);

            inGameInfo.getRoundInfo().put(i+1, Pair.of(gameMode, song));
        }
    }

    public void initializeSkipStatus(Room room) {
        InGameInfo inGameInfo = inGameMap.get(room.getId());
        for (Member member : room.getMembers()) {
            inGameInfo.getMemberSkipStatus().put(member.getId(), false);
        }
        inGameInfo.setSkipCount(0);
    }

    public void initializeRoundWinner(Room room) {
        InGameInfo inGameInfo = inGameMap.get(room.getId());
        inGameInfo.getRoundWinner().clear();
    }

    public InGameInfo getInGameInfo(Long roomId) {
        return inGameMap.get(roomId);
    }

    public int getCurrentRound(Long roomId) {
        return getInGameInfo(roomId).getCurrentRound();
    }

    public void nextRound(Long roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        inGameInfo.setCurrentRound(inGameInfo.getCurrentRound() + 1);
    }

    public void updateIsAnswered(Long roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        inGameInfo.setAnswered(!inGameInfo.isAnswered());
    }

    public Song getCurrentRoundSong(Long roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        int currentRound = inGameInfo.getCurrentRound();
        return inGameInfo.getRoundInfo().get(currentRound).getSecond();
    }

    public boolean isSkipped(Long roomId, Long memberId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getMemberSkipStatus().get(memberId);
    }

    public void setSkip(Long roomId, Long memberId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        inGameInfo.getMemberSkipStatus().put(memberId, true);
        inGameInfo.setSkipCount(inGameInfo.getSkipCount() + 1);
    }

    public int getSkipCount(Long roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getSkipCount();
    }

    public Map<Long, Boolean> getSkipStatus(Long roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getMemberSkipStatus();
    }

    public void removeSkipStatusWhoLeaved(Long roomId, Long memberId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        inGameInfo.getMemberSkipStatus().remove(memberId);
    }

    public void updateIsSongPlaying(Long roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        Map<Long, Boolean> isSongPlaying = inGameInfo.getIsSongPlaying();
        isSongPlaying.put(roomId, !isSongPlaying.getOrDefault(roomId, false));
    }

    public boolean getIsSongPlaying(Long roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getIsSongPlaying().get(roomId);
    }

    public void clear(Long roomId) {
        inGameMap.remove(roomId);
    }

    public Map<String, Integer> getScore(Long roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getScore();
    }

    public Map<Long, String> getRoundWinner(Long roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getRoundWinner();
    }

    public Map<String, Integer> getUserMovement(Long roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getUserMovement();
    }

    public Map<Integer, Pair<GameMode, Song>> getRoundInfo(Long roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getRoundInfo();
    }

    public Map<Long, Boolean> getMemberSkipStatus(Long roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getMemberSkipStatus();
    }

    public boolean isAnswered(Long roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.isAnswered();
    }
}
