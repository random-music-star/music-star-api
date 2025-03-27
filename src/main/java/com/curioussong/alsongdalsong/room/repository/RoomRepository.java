package com.curioussong.alsongdalsong.room.repository;

import com.curioussong.alsongdalsong.room.domain.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, String> {
    Optional<Room> findById(String id);
    Page<Room> findByStatusInOrderByUpdatedAtDesc(List<Room.RoomStatus> statuses, Pageable pageable);
}
