package com.curioussong.alsongdalsong.room.service;

import com.curioussong.alsongdalsong.game.domain.Game;
import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.game.domain.InGameManager;
import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.repository.GameRepository;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.repository.MemberRepository;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.dto.*;
import com.curioussong.alsongdalsong.room.event.RoomUpdatedEvent;
import com.curioussong.alsongdalsong.room.event.UserJoinedEvent;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import com.curioussong.alsongdalsong.roomgame.domain.RoomGame;
import com.curioussong.alsongdalsong.roomgame.repository.RoomGameRepository;
import com.curioussong.alsongdalsong.roomyear.domain.RoomYear;
import com.curioussong.alsongdalsong.roomyear.repository.RoomYearRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MemberRepository memberRepository;
    private final GameRepository gameRepository;
    private final RoomYearRepository roomYearRepository;
    private final RoomGameRepository roomGameRepository;
    private final RoomManager roomManager;
    private final InGameManager inGameManager;

    @Transactional
    public CreateResponse createRoom(Member member, CreateRequest request) {
        Room room = Room.builder()
                .host(member)
                .title(request.getTitle())
                .password(request.getPassword())
                .format(Room.RoomFormat.valueOf(request.getFormat()))
                .build();

        room.addMember(member);
        roomRepository.save(room);

        // GameMode 리스트를 기반으로 해당 Game 검색
        List<Game> games = request.getGameModes().stream()
                .map(gameMode -> gameRepository.findByMode(gameMode)
                        .orElseThrow(()-> new EntityNotFoundException("해당하는 게임이 없습니다.")))
                .toList();

        games.forEach(game -> roomGameRepository.save(new RoomGame(game, room)));

        request.getSelectedYears().forEach(year ->
                roomYearRepository.save(new RoomYear(room, year))
        );

        eventPublisher.publishEvent(new RoomUpdatedEvent(room));

        roomManager.addRoomInfo(room, request.getChannelId());

        return CreateResponse.builder()
                .roomId(room.getId())
                .build();
    }

    @Transactional
    public void joinRoom(String roomId, String sessionId, String userName) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("해당하는 방이 없습니다."));

        Member member = memberRepository.findByUsername(userName)
                .orElseThrow(() -> new EntityNotFoundException("해당 회원이 없습니다."));;

        room.addMember(member);

        eventPublisher.publishEvent(new UserJoinedEvent(room.getId(), sessionId, userName));
        eventPublisher.publishEvent(new RoomUpdatedEvent(room));
    }

    @Transactional
    public void leaveRoom(String roomId, String userName) {
        log.debug("방 나가기 시작 - roomId: {}, userName: {}", roomId, userName);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("해당하는 방이 없습니다."));

        log.debug("방 나가기 전 멤버 수: {}", room.getMembers().size());
        log.debug("방 멤버 목록: {}", room.getMembers().stream().map(Member::getUsername).toList());

        Member member = memberRepository.findByUsername(userName)
                .orElseThrow(() -> new EntityNotFoundException("해당 회원이 없습니다."));

        log.info("나가는 멤버: {}", member.getUsername());

        if (isHostLeaving(room, member)) {
            delegateHost(room, member);
        }

        room.removeMember(member);
        log.info("방 나가기 후 멤버 수: {}", room.getMembers().size());
        log.debug("방 멤버 목록 (나간 후): {}", room.getMembers().stream().map(Member::getUsername).toList());
        roomManager.getReadyStatus(roomId).remove(member.getId());
        if (room.getStatus() == Room.RoomStatus.IN_PROGRESS) {
            inGameManager.removeSkipStatusWhoLeaved(roomId, member.getId());
        }

        eventPublisher.publishEvent(new RoomUpdatedEvent(room, RoomUpdatedEvent.ActionType.DELETED));
        log.debug("이벤트 발행 완료");
    }

    private boolean isHostLeaving(Room room, Member member) {
        return member.getId().equals(room.getHost().getId());
    }

    private void delegateHost(Room room, Member member) {
        List<Member> members = room.getMembers();
        members.remove(member);
        if (!members.isEmpty()) {
            Member newHost = members.get(0);
            room.updateHost(newHost);
        } else {
            // 방 삭제
            room.updateStatus(Room.RoomStatus.FINISHED);
        }
    }

    @Transactional
    public void updateRoom(Member member, UpdateRequest request) {
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));

        Member host = room.getHost();
        log.debug("hostId:{}", host.getId());
        log.debug("memberId:{}", member.getId());
        if (host.getId() != member.getId()) {
            throw new IllegalArgumentException("방 설정은 방장만 변경 가능합니다.");
        }

        roomGameRepository.deleteAllByRoom(room);
        List<Game> games = request.getGameModes().stream()
                .map(gameMode -> gameRepository.findByMode(gameMode)
                        .orElseThrow(()-> new EntityNotFoundException("해당하는 게임이 없습니다.")))
                .toList();

        games.forEach(game -> roomGameRepository.save(new RoomGame(game, room)));

        room.update(request.getTitle(), request.getPassword(), Room.RoomFormat.valueOf(request.getFormat()));

        roomYearRepository.deleteAllByRoom(room);
        request.getSelectedYears().forEach(year ->
                roomYearRepository.save(new RoomYear(room, year))
        );

        roomManager.updateRoomInfo(room, request.getSelectedYears());

        eventPublisher.publishEvent(new RoomUpdatedEvent(room));
    }

    @Transactional(readOnly=true)
    public Page<RoomDTO> getRooms(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        List<Room.RoomStatus> activeStatuses = Arrays.asList(
                Room.RoomStatus.WAITING,
                Room.RoomStatus.IN_PROGRESS
        );

        Page<Room> roomPage = roomRepository.findByStatusInOrderByUpdatedAtDesc(activeStatuses, pageable);

        return roomPage.map(Room::toDto);
    }

    public boolean isRoomFull(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        return room.getMembers().size() == room.getMaxPlayer();
    }

    public boolean isRoomInProgress(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        return room.getStatus() == Room.RoomStatus.IN_PROGRESS;
    }

    @Transactional(readOnly=true)
    public LobbyResponse getRoomDataForLobby() {
        Page<RoomDTO> roomPage = getRooms(0, 8);

        for (RoomDTO roomDTO : roomPage.getContent()) {
            String roomId = roomDTO.getId();
            List<GameMode> gameModes = roomGameRepository.findGameModesByRoomId(roomId);
            roomDTO.setGameModes(gameModes);
        }

        return LobbyResponse.builder()
                .rooms(roomPage.getContent())
                .totalPages(roomPage.getTotalPages())
                .totalElements(roomPage.getTotalElements())
                .currentPage(roomPage.getNumber())
                .build();
    }

    public boolean isRoomFinished(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        return room.getStatus() == Room.RoomStatus.FINISHED;
    }

    @Transactional
    public EnterRoomResponse enterRoom(EnterRoomRequest request) {
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new HttpClientErrorException(
                        HttpStatus.BAD_REQUEST,
                        "존재하지 않는 방 아이디입니다."
                ));

        if(!room.getPassword().equals(request.getPassword())) {
            return EnterRoomResponse.builder()
                    .roomId(request.getRoomId())
                    .success(false)
                    .build();
        }

        return EnterRoomResponse.builder()
                .roomId(request.getRoomId())
                .success(true)
                .build();
    }
}