package com.curioussong.alsongdalsong.song.repository;

import com.curioussong.alsongdalsong.song.domain.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SongRepository extends JpaRepository<Song, Long> {

    @Query(value = "SELECT * FROM song WHERE year IN (:years) ORDER BY RAND() LIMIT :maxRound", nativeQuery = true)
    List<Song> findRandomSongsByYears(@Param("years") List<Long> years, @Param("maxRound") Integer maxRound);
}
