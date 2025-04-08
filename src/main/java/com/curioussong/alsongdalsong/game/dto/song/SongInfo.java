package com.curioussong.alsongdalsong.game.dto.song;

import com.curioussong.alsongdalsong.song.domain.Song;
import com.curioussong.alsongdalsong.ttssong.domain.TtsSong;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SongInfo {
    private Long id;
    private String korTitle;
    private String engTitle;
    private String artist;
    private String url;
    private Integer year;
    private Integer playTime;
    private SongType type;

    public static SongInfo fromSong(Song song) {
        return SongInfo.builder()
                .id(song.getId())
                .korTitle(song.getKorTitle())
                .engTitle(song.getEngTitle())
                .artist(song.getArtist())
                .url(song.getUrl())
                .year(song.getYear())
                .playTime(song.getPlayTime())
                .type(SongType.SONG)
                .build();
    }

    public static SongInfo fromTtsSong(TtsSong ttsSong) {
        return SongInfo.builder()
                .id(ttsSong.getId())
                .korTitle(ttsSong.getKorTitle())
                .engTitle(ttsSong.getEngTitle())
                .artist(ttsSong.getArtist())
                .url(ttsSong.getUrl())
                .year(ttsSong.getYear())
                .playTime(ttsSong.getPlayTime())
                .type(SongType.TTS_SONG)
                .build();
    }

    public Song toSong() {
        return Song.builder()
                .id(this.id)
                .korTitle(this.korTitle)
                .engTitle(this.engTitle)
                .artist(this.artist)
                .url(this.url)
                .year(this.year)
                .playTime(this.playTime)
                .build();
    }

    public TtsSong toTtsSong() {
        return TtsSong.builder()
                .id(this.id)
                .korTitle(this.korTitle)
                .engTitle(this.engTitle)
                .artist(this.artist)
                .url(this.url)
                .year(this.year)
                .playTime(this.playTime)
                .build();
    }
}