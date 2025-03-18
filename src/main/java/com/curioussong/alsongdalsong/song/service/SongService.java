package com.curioussong.alsongdalsong.song.service;

import com.curioussong.alsongdalsong.song.domain.Song;
import com.curioussong.alsongdalsong.song.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SongService {

    private final SongRepository songRepository;

    @Transactional
    public List<Song> getRandomSongByYear(List<Integer> years, Integer maxRound) {
        return songRepository.findRandomSongsByYears(years, maxRound);
    }
}
