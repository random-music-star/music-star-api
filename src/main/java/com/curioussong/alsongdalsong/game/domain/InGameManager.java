package com.curioussong.alsongdalsong.game.domain;

import com.curioussong.alsongdalsong.game.dto.song.SongInfo;
import com.curioussong.alsongdalsong.gamesession.domain.GameSession;
import com.curioussong.alsongdalsong.gamesession.repository.GameSessionRepository;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.roomgame.repository.RoomGameRepository;
import com.curioussong.alsongdalsong.song.domain.Song;
import com.curioussong.alsongdalsong.song.service.SongService;
import com.curioussong.alsongdalsong.ttssong.domain.TtsSong;
import com.curioussong.alsongdalsong.ttssong.repository.TtsSongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class InGameManager {

    private final RoomGameRepository roomGameRepository;

    private final Map<String, InGameInfo> inGameMap = new ConcurrentHashMap<>();
    private final SongService songService;
    private final RoomManager roomManager;
    private final TtsSongRepository ttsSongRepository;
    private final Map<String, Long> sessionIdMap = new ConcurrentHashMap<>();
    private final GameSessionRepository gameSessionRepository;
    private final Map<String, String> submittedAnswerMap = new ConcurrentHashMap<>();

    public void initializeGameSettings(Room room) {
        InGameInfo inGameInfo = new InGameInfo();
        inGameMap.put(room.getId(), inGameInfo);
        inGameInfo.setCurrentRound(1); // 라운드 초기화
        initializeScores(inGameInfo, room); // 점수 초기화
        initializeRoundWinner(room); // 정답자 초기화
        initializeUserMovement(inGameInfo, room); // 사용자 별 움직이는거리 초기화
        initializeSongs(inGameInfo, room); // 노래 세팅
        initializeSkipStatus(room); // 스킵 상태 초기화
        initializeGameSessionId(room);
        initializeIsSongPlaying(room);
    }

    private void initializeGameSessionId(Room room) {
        GameSession gameSession = gameSessionRepository.findTopByRoomIdOrderByStartTimeDesc(room.getId())
                .orElseThrow(() -> new IllegalStateException("해당 room에 대한 game session을 찾을 수 없습니다."));
        sessionIdMap.putIfAbsent(room.getId(), gameSession.getId());
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
        List<GameMode> gameModes = roomGameRepository.findGameModesByRoomId(room.getId());

        int maxSongCount = room.getMaxGameRound()*2;
        List<Song> selectedNormalSongs = songService.getRandomSongByYear(roomInfo.getSelectedYears(), maxSongCount);
        List<TtsSong> selectedTtsSongs = ttsSongRepository.findRandomTtsSongsByYears(roomInfo.getSelectedYears(), room.getMaxGameRound());

        List<SongInfo> selectedNormalSongsInfo = new ArrayList<>();
        for (Song normalSong : selectedNormalSongs) {
            selectedNormalSongsInfo.add(SongInfo.fromSong(normalSong));
        }

        List<SongInfo> selectedTtsSongsInfo = new ArrayList<>();
        for (TtsSong selectedTtsSong : selectedTtsSongs) {
            selectedTtsSongsInfo.add(SongInfo.fromTtsSong(selectedTtsSong));
        }

        int songIndex = 0;
        int ttsIndex = 0;
        for (int round = 1; round <= room.getMaxGameRound(); round++) {
            GameMode gameMode = getRandomGameMode(gameModes);

            if(gameMode == GameMode.DUAL){
                SongInfo firstSong = selectSongForRound(gameMode, selectedNormalSongsInfo, selectedTtsSongsInfo, songIndex++);
                SongInfo secondSong = selectSongForRound(gameMode, selectedNormalSongsInfo, selectedTtsSongsInfo, songIndex++);

                inGameInfo.getRoundInfo().put(round, SongPair.createDual(gameMode, firstSong, secondSong));
            } else if(gameMode == GameMode.TTS) {
                SongInfo selectedSong = selectSongForRound(gameMode, selectedNormalSongsInfo, selectedTtsSongsInfo, ttsIndex++);
                inGameInfo.getRoundInfo().put(round, SongPair.createSingle(gameMode, selectedSong));
            } else {
                SongInfo selectedSong = selectSongForRound(gameMode, selectedNormalSongsInfo, selectedTtsSongsInfo, songIndex++);
                inGameInfo.getRoundInfo().put(round, SongPair.createSingle(gameMode, selectedSong));
            }
        }
    }

    private void initializeIsSongPlaying(Room room) {
        inGameMap.get(room.getId()).getIsSongPlaying().put(room.getId(), false);
    }

    private GameMode getRandomGameMode(List<GameMode> gameModes) {
        SecureRandom random = new SecureRandom();
        return gameModes.get(random.nextInt(gameModes.size()));
    }

    private SongInfo selectSongForRound(GameMode gameMode, List<SongInfo> normalSongsInfo, List<SongInfo> ttsSongsInfo, int index) {
        if (gameMode == GameMode.TTS) {
            return ttsSongsInfo.get(index);
        }
        return normalSongsInfo.get(index); // GameMode.FULL
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

    public InGameInfo getInGameInfo(String roomId) {
        return inGameMap.get(roomId);
    }

    public int getCurrentRound(String roomId) {
        return getInGameInfo(roomId).getCurrentRound();
    }

    public void nextRound(String roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        inGameInfo.setCurrentRound(inGameInfo.getCurrentRound() + 1);
    }

    public void markAsAnswered(String roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        inGameInfo.setAnswered(true);
    }

    public void resetAnswered(String roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        inGameInfo.setAnswered(false);
    }

    public SongInfo getCurrentRoundSong(String roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        int currentRound = inGameInfo.getCurrentRound();
        return inGameInfo.getRoundInfo().get(currentRound).getFirstSong();
    }

    public SongInfo getSecondSongForCurrentRound(String roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        int currentRound = inGameInfo.getCurrentRound();
        SongPair songPair = inGameInfo.getRoundInfo().get(currentRound);

        if(songPair.hasSecondSong()){
            return songPair.getSecondSong();
        }
        return null;
    }

    public boolean hasSecondSongInCurrentRound(String roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        int currentRound = inGameInfo.getCurrentRound();
        return inGameInfo.getRoundInfo().get(currentRound).hasSecondSong();
    }

    public GameMode getCurrentRoundGameMode(String roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        int currentRound = inGameInfo.getCurrentRound();
        return inGameInfo.getRoundInfo().get(currentRound).getGameMode();
    }

    public boolean isSkipped(String roomId, Long memberId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getMemberSkipStatus().get(memberId);
    }

    public void setSkip(String roomId, Long memberId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        inGameInfo.getMemberSkipStatus().put(memberId, true);
        inGameInfo.setSkipCount(inGameInfo.getSkipCount() + 1);
    }

    public int getSkipCount(String roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getSkipCount();
    }

    public Map<Long, Boolean> getSkipStatus(String roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getMemberSkipStatus();
    }

    public void removeSkipStatusWhoLeaved(String roomId, Long memberId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        inGameInfo.getMemberSkipStatus().remove(memberId);
    }

    public void updateIsSongPlaying(String roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        Map<String, Boolean> isSongPlaying = inGameInfo.getIsSongPlaying();
        isSongPlaying.put(roomId, !isSongPlaying.getOrDefault(roomId, false));
    }

    public boolean getIsSongPlaying(String roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getIsSongPlaying().get(roomId);
    }

    public void clear(String roomId) {
        inGameMap.remove(roomId);
        sessionIdMap.remove(roomId);
    }

    public Map<String, Integer> getScore(String roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getScore();
    }

    public Map<String, String> getRoundWinner(String roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getRoundWinner();
    }

    public Map<String, Integer> getUserMovement(String roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getUserMovement();
    }

    public Map<Integer, SongPair> getRoundInfo(String roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getRoundInfo();
    }

    public Map<Long, Boolean> getMemberSkipStatus(String roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.getMemberSkipStatus();
    }

    public boolean isAnswered(String roomId) {
        InGameInfo inGameInfo = getInGameInfo(roomId);
        return inGameInfo.isAnswered();
    }

    public Long getGameSessionId(String roomId) {
        return sessionIdMap.get(roomId);
    }

    public void setSubmittedAnswer(String roomId, String answer) {
        submittedAnswerMap.put(roomId, answer);
    }

    public String getSubmittedAnswer(String roomId) {
        return submittedAnswerMap.get(roomId);
    }

    public void clearSubmittedAnswer(String id) {
        submittedAnswerMap.remove(id);
    }
}
