package com.curioussong.alsongdalsong.song;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name="song")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kor_title", nullable = false, length = 50)
    private String korTitle;

    @Column(name = "eng_title", length = 50)
    private String engTitle;

    @Column(name = "artist", nullable = false, length = 50)
    private String artist;

    @Column(name = "genre", nullable = false, length = 25)
    private String genre;

    @Column(name = "youtube_url", nullable = false)
    private String youtubeUrl;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Builder
    public Song(String korTitle, String engTitle, String artist, String genre, String youtubeUrl, Integer year) {
        this.korTitle = korTitle;
        this.engTitle = engTitle;
        this.artist = artist;
        this.genre = genre;
        this.youtubeUrl = youtubeUrl;
        this.year = year;
    }
}
