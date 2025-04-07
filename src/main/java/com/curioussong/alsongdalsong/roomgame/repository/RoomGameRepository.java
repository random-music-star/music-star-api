package com.curioussong.alsongdalsong.roomgame.repository;

import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.roomgame.domain.RoomGame;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomGameRepository extends JpaRepository<RoomGame, Long> {

    List<RoomGame> findByRoomId(String roomId);

    @Query("SELECT rg.game.mode FROM RoomGame rg WHERE rg.room.id = :roomId")
    List<GameMode> findGameModesByRoomId(@Param("roomId") String roomId);

    void deleteAllByRoom(Room room);

    @EntityGraph(attributePaths = {"game", "room"})
    List<RoomGame> findByRoomIdIn(List<String> roomIds);
}
