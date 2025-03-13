package com.curioussong.alsongdalsong.roomgame.repository;

import com.curioussong.alsongdalsong.roomgame.domain.RoomGame;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomGameRepository extends JpaRepository<RoomGame, Long> {

    RoomGame findByRoomId(Long roomId);
}
