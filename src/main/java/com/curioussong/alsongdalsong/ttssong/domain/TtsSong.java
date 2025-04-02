package com.curioussong.alsongdalsong.ttssong.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name="tts_song")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TtsSong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kor_title", nullable = false, length = 50)
    private String korTitle;

    @Column(name = "eng_title", length = 50)
    private String engTitle;

    @Column(name = "artist", nullable = false, length = 50)
    private String artist;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "year", nullable = false, columnDefinition = "YEAR")
    private Integer year;

    @Column(name = "play_time", nullable = false)
    private Integer playTime;

    @Builder
    public TtsSong(String korTitle, String engTitle, String artist, String url, Integer year, Integer playTime) {
        this.korTitle = korTitle;
        this.engTitle = engTitle;
        this.artist = artist;
        this.url = url;
        this.year = year;
        this.playTime = playTime;
    }

}
