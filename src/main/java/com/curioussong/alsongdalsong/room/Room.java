package com.curioussong.alsongdalsong.room;

import com.curioussong.alsongdalsong.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "room")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private Member host;

    @Column(name = "password", length = 30)
    private String password;

    @Column(name = "max_player", nullable = false)
    private Integer maxPlayer;

    @Column(name = "max_game_round", nullable = false)
    private Integer maxGameRound;

    @Column(name = "format", nullable = false, length = 25)
    private String format;

    @Column(name = "play_time", nullable = false)
    private Integer playTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoomStatus status;

    @OneToMany
    @JoinColumn(name = "room_id")
    private List<Member> members = new ArrayList<>();

    public enum RoomStatus {
        WAITING, IN_PROGRESS, FINISHED
    }

    @Builder
    public Room(Member host, String password, Integer maxPlayer, Integer maxGameRound, String format, Integer playTime, RoomStatus status) {
        this.host = host;
        this.password = password;
        this.maxPlayer = maxPlayer;
        this.maxGameRound = maxGameRound;
        this.format = format;
        this.playTime = playTime;
        this.status = status;
    }
}
