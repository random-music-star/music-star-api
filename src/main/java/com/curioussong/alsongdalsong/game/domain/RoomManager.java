package com.curioussong.alsongdalsong.game.domain;

import com.curioussong.alsongdalsong.game.dto.roominfo.RoomInfoResponse;
import com.curioussong.alsongdalsong.game.dto.roominfo.RoomInfoResponseDTO;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import com.curioussong.alsongdalsong.roomgame.repository.RoomGameRepository;
import com.curioussong.alsongdalsong.roomyear.domain.RoomYear;
import com.curioussong.alsongdalsong.roomyear.repository.RoomYearRepository;
import com.curioussong.alsongdalsong.song.domain.Song;
import com.curioussong.alsongdalsong.song.service.SongService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomManager {

    private final Map<Long, RoomInfo> roomMap = new ConcurrentHashMap<>();

    private final SongService songService;
    private final RoomYearRepository roomYearRepository;
    private final RoomRepository roomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomGameRepository roomGameRepository;

    // 방 정보 반환
    public RoomInfo getRoomInfo(Long roomId) {
        return roomMap.get(roomId);
    }

    // 방 정보 추가
    public void addRoomInfo(Room room, Long channelId) {
        RoomInfo roomInfo = new RoomInfo(room.getId(), channelId);

        // 기존 방 정보를 바탕으로 RoomInfo 초기화
        roomInfo.setMaxGameRound(room.getMaxGameRound());

        log.info("Members 호출 확인 : {} 명", room.getMembers().size());

        // 멤버 목록을 가져와서 Ready 상태 & Skip 상태 초기화
        for (Member member : room.getMembers()) {
            roomInfo.getMemberReadyStatus().put(member.getId(), false);
            roomInfo.getMemberSkipStatus().put(member.getId(), false);
        }

        // 노래 년도 가져오기
        roomInfo.setSelectedYears(getSelectedYearsFromRoomYear(room));

        roomMap.put(room.getId(), roomInfo);
    }

    public List<Integer> getSelectedYearsFromRoomYear(Room room) {
        return roomYearRepository.findAllByRoom(room)
                .stream()
                .map(RoomYear::getYear)
                .collect(Collectors.toList());
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
        List<GameMode> gameModes = roomGameRepository.findGameModesByRoomId(roomId);
        Random random = new Random();
        for (int i = 0; i < maxRound; i++) {
            int randomIndex = random.nextInt(gameModes.size());
            GameMode gameMode = gameModes.get(randomIndex);
            Song song = selectedSongs.get(i);

            // RoomInfo의 roundToInfo 맵에 게임 모드와 노래 저장
            roomInfo.getRoundToInfo().put(i+1, Pair.of(gameMode, song));
        }

        // 라운드 초기화
        roomInfo.setCurrentRound(1);


        initializeScores(roomId); // 사용자 점수 초기화
        initializeRoundWinner(roomId); // 정답자 초기화
        initializeUserMovement(roomId); // 사용자 별 움직이는 거리 초기화
    }

    private void initializeUserMovement(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        Map<String, Integer> userMovement = getRoomInfo(roomId).getUserMovement();

        room.getMembers().forEach(member -> {userMovement.put(member.getUsername(), 0);});

    }

    private void initializeRoundWinner(Long roomId) {
        RoomInfo roomInfo = getRoomInfo(roomId);
        roomInfo.getRoundWinner().clear();
    }

    private void initializeScores(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        List<Member> members = room.getMembers();

        Map<String, Integer> userScore = getRoomInfo(roomId).getScore();
        userScore.clear();

        for (Member member : members) {
            userScore.put(member.getUsername(), 0);
        }
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

    public Map<Long, Boolean> getSkipStatus(Long roomId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        return roomInfo.getMemberSkipStatus();
    }

    public void deleteMember(Long roomId, Long memberId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        roomInfo.getMemberSkipStatus().remove(memberId);
        roomInfo.getMemberReadyStatus().remove(memberId);
    }

    public void setSelectedYears(Long roomId, List<Integer> selectedYears) {
        RoomInfo roomInfo = roomMap.get(roomId);
        roomInfo.setSelectedYears(selectedYears);
    }

    public void updateRoomInfo(Room room, List<Integer> updatedSelectedYears) {
        Long roomId = room.getId();
        RoomInfo roomInfo = roomMap.get(roomId);

        log.info("Updating RoomInfo for roomId: {}", roomId);
        roomInfo.setSelectedYears(updatedSelectedYears);


        String destination = String.format("/topic/channel/%d/room/%d", roomInfo.getChannelId(), roomId);
        List<GameMode> gameModes = roomGameRepository.findGameModesByRoomId(roomId);


        messagingTemplate.convertAndSend(destination, RoomInfoResponseDTO.builder()
                .type("roomInfo")
                .response(RoomInfoResponse.builder()
                        .roomTitle(room.getTitle())
                        .maxPlayer(room.getMaxPlayer())
                        .maxGameRound(room.getMaxGameRound())
                        .hasPassword(room.getPassword()!=null&&!room.getPassword().isEmpty())
                        .format(room.getFormat())
                        .status(room.getStatus())
                        .mode(gameModes)
                        .selectedYear(updatedSelectedYears)
                        .build())
                .build());
    }

    public void updateIsSongPlaying(Long roomId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        Map<Long, Boolean> isSongPlaying = roomInfo.getIsSongPlaying();
        isSongPlaying.put(roomId, !isSongPlaying.getOrDefault(roomId, false));
    }

    public boolean getIsSongPlaying(Long roomId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        return roomInfo.getIsSongPlaying().get(roomId);
    }
}
