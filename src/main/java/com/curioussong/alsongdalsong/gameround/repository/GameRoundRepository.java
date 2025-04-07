package com.curioussong.alsongdalsong.gameround.repository;

import com.curioussong.alsongdalsong.gameround.domain.GameRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRoundRepository extends JpaRepository<GameRound, Long> {
    Optional<GameRound> findByGameSessionIdAndRoundNumber(Long gameSessionId, int roundNumber);
}
