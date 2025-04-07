package com.curioussong.alsongdalsong.gamesession.domain;

import com.curioussong.alsongdalsong.gameround.domain.GameRound;
import com.curioussong.alsongdalsong.gamesessionmode.domain.GameSessionMode;
import com.curioussong.alsongdalsong.gamesessionyear.domain.GameSessionYear;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.room.domain.Room;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@Table(name = "game_session")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", length = 26)
    private String roomId;

    @Column(name = "title", nullable = false, length = 15)
    private String title;

    @Column(name = "max_player", nullable = false)
    private Integer maxPlayer;

    @Column(name = "max_game_round", nullable = false)
    private Integer maxGameRound;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false)
    private Room.RoomFormat format;

    @Column(name = "password", length = 30)
    private String password;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "initial_participant_count", nullable = false)
    private Integer initialParticipantCount;

    @Column(name = "final_participant_count")
    private Integer finalParticipantCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private Member host;

    public void finishSession(Integer finalParticipantCount) {
        this.endTime = LocalDateTime.now();
        this.finalParticipantCount = finalParticipantCount;
    }
}
