package com.curioussong.alsongdalsong.game.domain;

import com.curioussong.alsongdalsong.game.dto.song.SongInfo;
import com.curioussong.alsongdalsong.song.domain.Song;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SongPair {
    private GameMode gameMode;
    private SongInfo firstSong;
    private SongInfo secondSong;

    public boolean hasSecondSong() {
        return secondSong != null;
    }

    public static SongPair createSingle(GameMode gameMode, SongInfo song) {
        return SongPair.builder()
                .gameMode(gameMode)
                .firstSong(song)
                .build();
    }

    public static SongPair createDual(GameMode gameMode, SongInfo firstSong, SongInfo secondSong) {
        return SongPair.builder()
                .gameMode(gameMode)
                .firstSong(firstSong)
                .secondSong(secondSong)
                .build();
    }
}
