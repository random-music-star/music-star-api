package com.curioussong.alsongdalsong.gamesession.repository;

import com.curioussong.alsongdalsong.gamesession.domain.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    Optional<GameSession> findTopByRoomIdOrderByStartTimeDesc(String roomId);
}
