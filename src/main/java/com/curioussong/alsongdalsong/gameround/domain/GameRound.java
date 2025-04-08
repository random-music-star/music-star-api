package com.curioussong.alsongdalsong.gameround.domain;

import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.gamesession.domain.GameSession;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.song.domain.Song;
import com.curioussong.alsongdalsong.ttssong.domain.TtsSong;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@Table(name = "game_round")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class GameRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode", nullable = false)
    private GameMode gameMode;

    @Column(name = "year", nullable = false)
    private Integer year;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id")
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "second_song_id")
    private Song secondSong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tts_song_id")
    private TtsSong ttsSong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Member winner;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "submitted_answer", length = 100)
    private String submittedAnswer;

    public void finish(LocalDateTime endTime, Member winner, String submittedAnswer) {
        this.endTime = endTime;
        this.winner = winner;
        this.submittedAnswer = submittedAnswer;
    }
}
