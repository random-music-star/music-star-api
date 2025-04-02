package com.curioussong.alsongdalsong.song.service;

import com.curioussong.alsongdalsong.song.domain.Song;
import com.curioussong.alsongdalsong.song.repository.SongRepository;
import com.curioussong.alsongdalsong.ttssong.domain.TtsSong;
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

    public Song ttsSongToSong(TtsSong ttsSong) {
        return Song.builder()
                .korTitle(ttsSong.getKorTitle())
                .engTitle(ttsSong.getEngTitle())
                .artist(ttsSong.getArtist())
                .url(ttsSong.getUrl())
                .year(ttsSong.getYear())
                .playTime(ttsSong.getPlayTime())
                .build();
    }
}
