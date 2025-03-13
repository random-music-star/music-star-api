package com.curioussong.alsongdalsong.roomgame.service;

import com.curioussong.alsongdalsong.roomgame.domain.RoomGame;
import com.curioussong.alsongdalsong.roomgame.repository.RoomGameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomGameService {

    private final RoomGameRepository roomGameRepository;

    @Transactional
    public RoomGame getRoomGameByRoomId(Long roomId) {
        return roomGameRepository.findByRoomId(roomId);
    }
}
