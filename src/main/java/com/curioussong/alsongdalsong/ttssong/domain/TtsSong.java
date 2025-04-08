package com.curioussong.alsongdalsong.ttssong.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@Table(name="tts_song")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
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
}
