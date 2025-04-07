package com.curioussong.alsongdalsong.game.domain;

import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfo;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.roomgame.repository.RoomGameRepository;
import com.curioussong.alsongdalsong.roomyear.domain.RoomYear;
import com.curioussong.alsongdalsong.roomyear.repository.RoomYearRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomManager {

    private final Map<String, RoomInfo> roomMap = new ConcurrentHashMap<>();

    private final RoomYearRepository roomYearRepository;
    private final RoomGameRepository roomGameRepository;
    private final GameMessageSender gameMessageSender;

    // 방 정보 반환
    public RoomInfo getRoomInfo(String roomId) {
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

        roomInfo.setSelectedYears(getSelectedYearsFromRoomYear(room));
        roomInfo.setGameModes(findGameModesByRoom(room));

        roomMap.put(room.getId(), roomInfo);
    }

    public Map<String, String> getAuthorizedUser(String roomId) {
        return roomMap.get(roomId).getAuthorizedUser();
    }

    public void authorizeUser(String roomId, String userName) {
        roomMap.get(roomId).getAuthorizedUser().put(userName, roomId);
    }

    public void removeAuthorizedUser(String authorizedUser, Room room) {
        getAuthorizedUser(room.getId()).remove(authorizedUser);
    }

    public List<Integer> getSelectedYearsFromRoomYear(Room room) {
        return roomYearRepository.findAllByRoom(room)
                .stream()
                .map(RoomYear::getYear)
                .collect(Collectors.toList());
    }

    private List<GameMode> findGameModesByRoom(Room room) {
        return roomGameRepository.findGameModesByRoomId(room.getId());
    }

    // 방의 노래년도 리스트 반환
    public List<Integer> getSelectedYears(String roomId) {
        return getRoomInfo(roomId).getSelectedYears();
    }

    public List<GameMode> getGameModes(String roomId) {
        return getRoomInfo(roomId).getGameModes();
    }

    // 방의 멤버 레디 상태 초기화 (ready → false)
    public void initializeMemberReadyStatus(String roomId) {
        RoomInfo roomInfo = getRoomInfo(roomId);
        Map<Long, Boolean> readyMember = roomInfo.getMemberReadyStatus();
        for (Long memberId : readyMember.keySet()) {
            readyMember.put(memberId, false);
        }
    }

    // 방 레디 체크
    public boolean areAllPlayersReady(String roomId) {
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
    public boolean getReady(String roomId, Long memberId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        return roomInfo.getMemberReadyStatus().get(memberId);
    }

    // Ready 상태 Map 반환
    public Map<Long, Boolean> getReadyStatus(String roomId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        return roomInfo.getMemberReadyStatus();
    }

    public void setSelectedYears(String roomId, List<Integer> selectedYears) {
        RoomInfo roomInfo = roomMap.get(roomId);
        roomInfo.setSelectedYears(selectedYears);
    }

    public void updateRoomInfo(Room room, List<Integer> updatedSelectedYears) {
        RoomInfo roomInfo = getRoomInfo(room.getId());

        List<Integer> selectedYears = roomInfo.getSelectedYears();
        selectedYears.clear();
        selectedYears.addAll(updatedSelectedYears);

        List<GameMode> gameModes = roomGameRepository.findGameModesByRoomId(room.getId());

        String destination = String.format("/topic/channel/%d/room/%s", roomInfo.getChannelId(), room.getId());

        gameMessageSender.sendRoomInfo(destination, room, roomInfo.getSelectedYears(), gameModes);
    }

    public int getMaxGameRound(String roomId) {
        RoomInfo roomInfo = roomMap.get(roomId);
        return roomInfo.getMaxGameRound();
    }

    public List<UserInfo> getUserInfos(Room room) {
        List<UserInfo> userInfoList = new ArrayList<>();

        for (Member member : room.getMembers()) {
            boolean isHost = member.getId().equals(room.getHost().getId());
            boolean isReady = Boolean.TRUE.equals(getReady(room.getId(), member.getId()));
            String colorCode = member.getColorCode();

            userInfoList.add(new UserInfo(member.getUsername(), isReady, isHost, colorCode));
        }

        return userInfoList;
    }

    public boolean isAllReady(Room room) {
        List<UserInfo> userInfos = getUserInfos(room);
        for (UserInfo userInfo : userInfos) {
            if (userInfo.getIsReady() == Boolean.FALSE) {
                return false;
            }
        }

        return true;
    }
}
