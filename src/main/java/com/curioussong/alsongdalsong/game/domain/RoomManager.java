package com.curioussong.alsongdalsong.game.domain;

import com.curioussong.alsongdalsong.game.dto.roominfo.RoomInfoResponse;
import com.curioussong.alsongdalsong.game.dto.roominfo.RoomInfoResponseDTO;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.roomgame.repository.RoomGameRepository;
import com.curioussong.alsongdalsong.roomyear.domain.RoomYear;
import com.curioussong.alsongdalsong.roomyear.repository.RoomYearRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomManager {

    private final Map<Long, RoomInfo> roomMap = new ConcurrentHashMap<>();

    private final RoomYearRepository roomYearRepository;
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

    // 방의 노래년도 리스트 반환
    public List<Integer> getSelectedYears(Long roomId) {
        return getRoomInfo(roomId).getSelectedYears();
    }

    // 방의 멤버 레디 상태 초기화 (ready → false)
    public void initializeMemberReadyStatus(Long roomId) {
        RoomInfo roomInfo = getRoomInfo(roomId);
        Map<Long, Boolean> readyMember = roomInfo.getMemberReadyStatus();
        for (Long memberId : readyMember.keySet()) {
            readyMember.put(memberId, false);
        }
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

    // 해당 멤버의 ready 상태 반환
    public boolean getReady(Long roomId, Long memberId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        return roomInfo.getMemberReadyStatus().get(memberId);
    }

    // Ready 상태 Map 반환
    public Map<Long, Boolean> getReadyStatus(Long roomId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        return roomInfo.getMemberReadyStatus();
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
}
