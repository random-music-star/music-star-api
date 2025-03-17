package com.curioussong.alsongdalsong.room.service;

import com.curioussong.alsongdalsong.game.domain.Game;
import com.curioussong.alsongdalsong.game.event.YearSelectionEvent;
import com.curioussong.alsongdalsong.game.service.GameService;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.repository.MemberRepository;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.dto.CreateRequest;
import com.curioussong.alsongdalsong.room.dto.CreateResponse;
import com.curioussong.alsongdalsong.room.dto.RoomDTO;
import com.curioussong.alsongdalsong.room.dto.UpdateRequest;
import com.curioussong.alsongdalsong.room.event.RoomUpdatedEvent;
import com.curioussong.alsongdalsong.room.event.UserJoinedEvent;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import com.curioussong.alsongdalsong.roomgame.domain.RoomGame;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MemberRepository memberRepository;
    private final GameService gameService;

    @Transactional
    public CreateResponse createRoom(Member member, CreateRequest request) {
        Room room = Room.builder()
                .host(member)
                .title(request.getTitle())
                .password(request.getPassword())
                .format(Room.RoomFormat.valueOf(request.getFormat()))
                .build();

        room.addMember(member);

        // GameMode 리스트를 기반으로 해당 Game 검색
        List<Game> games = request.getGameModes().stream()
                .map(gameService::getGameByMode)
                .toList();

        // roomgame과 연관관계 설정
        for (Game game : games) {
            RoomGame roomgame = RoomGame.builder()
                    .room(room)
                    .game(game)
                    .build();
            roomgame.setGame(game);
            roomgame.setRoom(room);
            room.addRoomGame(roomgame);
        }

        roomRepository.save(room);

        eventPublisher.publishEvent(new RoomUpdatedEvent(room.getId()));
        eventPublisher.publishEvent(new YearSelectionEvent(room.getId(), request.getSelectedYears()));

//      Todo room 생성시 이벤트 등록하고 추후 game 서비스에서 받아 엔티티 형성
        return CreateResponse.builder()
                .roomId(room.getId())
                .build();
    }

    @Transactional
    public void joinRoom(Long roomId, String userName) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("해당하는 방이 없습니다."));

        Member member = memberRepository.findByUsername(userName);

        room.addMember(member);

        eventPublisher.publishEvent(new UserJoinedEvent(room.getId(), userName));
    }

    @Transactional
    public void updateRoom(Member member, UpdateRequest request) {
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));

        Member host = room.getHost();
        log.debug("hostId:{}", host.getId());
        log.debug("memberId:{}", member.getId());
        if(host.getId()!=member.getId()) {
            throw new IllegalArgumentException("방 설정은 방장만 변경 가능합니다.");
        }

        eventPublisher.publishEvent(new RoomUpdatedEvent(room.getId()));
        eventPublisher.publishEvent(new YearSelectionEvent(room.getId(), request.getSelectedYears()));
        room.update(request.getTitle(), request.getPassword(), Room.RoomFormat.valueOf(request.getFormat()));
    }

    @Transactional
    public Room findRoomById(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("해당하는 방이 없습니다."));
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

    public boolean isRoomFull(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        return room.getMembers().size() == room.getMaxPlayer();
    }

    public boolean isRoomInProgress(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        return room.getStatus() == Room.RoomStatus.IN_PROGRESS;
    }
}
