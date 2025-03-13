package com.curioussong.alsongdalsong.game.repository;

import com.curioussong.alsongdalsong.game.domain.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findById(Long id);
}
