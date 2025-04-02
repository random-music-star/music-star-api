package com.curioussong.alsongdalsong.ttssong.repository;

import com.curioussong.alsongdalsong.ttssong.domain.TtsSong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TtsSongRepository extends JpaRepository<TtsSong, Long> {

    @Query(value = "SELECT * FROM tts_song WHERE year IN (:years) ORDER BY RAND() LIMIT :maxRound", nativeQuery = true)
    List<TtsSong> findRandomTtsSongsByYears(@Param("years") List<Integer> years, @Param("maxRound") Integer maxRound);
}
