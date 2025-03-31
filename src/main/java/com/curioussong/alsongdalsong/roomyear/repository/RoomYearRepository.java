package com.curioussong.alsongdalsong.roomyear.repository;

import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.roomyear.domain.RoomYear;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomYearRepository extends JpaRepository<RoomYear, Long> {

    List<RoomYear> findAllByRoom(Room room);

    void deleteAllByRoom(Room room);

    @EntityGraph(attributePaths = {"room"})
    List<RoomYear> findByRoomIdIn(List<String> roomIds);
}
