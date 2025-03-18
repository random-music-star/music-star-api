package com.curioussong.alsongdalsong.game.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "game")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false)
    private GameMode mode;

    @Column(name = "hint_artist_time", nullable = false)
    private Integer hintArtistTime;

    @Column(name = "hint_consonant_time", nullable = false)
    private Integer hintConsonantTime;

    @Builder
    public Game(GameMode mode, Integer hintArtistTime, Integer hintConsonantTime) {
        this.mode = mode;
        this.hintArtistTime = hintArtistTime;
        this.hintConsonantTime = hintConsonantTime;
    }
}
