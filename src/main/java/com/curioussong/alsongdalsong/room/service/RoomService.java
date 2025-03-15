package com.curioussong.alsongdalsong.room.service;

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

        eventPublisher.publishEvent(new RoomUpdatedEvent(room.getId()));

//      Todo room 생성시 이벤트 등록하고 추후 game 서비스에서 받아 엔티티 형성
        return CreateResponse.builder()
                .roomId(room.getId())
                .build();
    }

    @Transactional
    public void joinRoom(Long roomId, String username) {
        Room room = roomRepository.findById(roomId).orElse(null);

        Member member = memberRepository.findByUsername(username);

        // 방에 이미 최대인원이 있는지 나중에 검증 필요
        room.getMembers().add(member);

        eventPublisher.publishEvent(new UserJoinedEvent(room.getId(), username));
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
        room.update(request.getTitle(), request.getPassword(), Room.RoomFormat.valueOf(request.getFormat()));
    }

    @Transactional
    public Room findRoomById(Long id) {
        return roomRepository.findById(id).orElse(null);
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
}
