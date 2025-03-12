package com.curioussong.alsongdalsong.room.service;

import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.dto.CreateRequest;
import com.curioussong.alsongdalsong.room.dto.CreateResponse;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

//  Todo member 실제 방 생성 member로 적용
    @Transactional
    public CreateResponse createRoom(CreateRequest request) {
        Room room = Room.builder()
//                .host(member_id)
                .title(request.getTitle())
                .password(request.getPassword())
                .format(Room.RoomFormat.valueOf(request.getFormat()))
                .build();

        roomRepository.save(room);

//      Todo room 생성시 이벤트 등록하고 추후 game 서비스에서 받아 엔티티 형성
        return CreateResponse.builder()
                .roomId(room.getId())
                .build();
    }
}
