package com.curioussong.alsongdalsong.game.domain;

import com.curioussong.alsongdalsong.song.domain.Song;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SongPair {
    private GameMode gameMode;
    private Song firstSong;
    private Song secondSong;

    public boolean hasSecondSong() {
        return secondSong != null;
    }

    public static SongPair createSingle(GameMode gameMode, Song song) {
        return SongPair.builder()
                .gameMode(gameMode)
                .firstSong(song)
                .build();
    }

    public static SongPair createDual(GameMode gameMode, Song firstSong, Song secondSong) {
        return SongPair.builder()
                .gameMode(gameMode)
                .firstSong(firstSong)
                .secondSong(secondSong)
                .build();
    }
}
