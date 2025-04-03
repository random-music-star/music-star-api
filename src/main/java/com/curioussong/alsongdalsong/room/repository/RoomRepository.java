package com.curioussong.alsongdalsong.room.repository;

import com.curioussong.alsongdalsong.room.domain.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, String> {
    Optional<Room> findById(String id);
    Page<Room> findByChannelIdAndStatusInOrderByUpdatedAtDesc(
            Long channelId,
            List<Room.RoomStatus> statuses,
            Pageable pageable
    );
    @Query("SELECT MAX(r.roomNumber) FROM Room r WHERE r.channel.id = :channelId")
    Long findMaxRoomNumberByChannelId(@Param("channelId") Long channelId);

    List<Room> findByChannelIdAndTitleContainingAndStatusOrderByUpdatedAtDesc(
            Long channelId,
            String title,
            Room.RoomStatus statuses);

    Optional<Room> findByChannelIdAndRoomNumberAndStatusOrderByUpdatedAtDesc(
            Long channelId,
            Long roomNumber,
            Room.RoomStatus statuses
    );
}
