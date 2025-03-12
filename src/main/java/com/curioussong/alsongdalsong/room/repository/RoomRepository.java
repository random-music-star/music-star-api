package com.curioussong.alsongdalsong.room.repository;

import com.curioussong.alsongdalsong.room.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
