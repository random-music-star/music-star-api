package com.curioussong.alsongdalsong.room.domain;

import com.curioussong.alsongdalsong.member.domain.Member;
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
    @JoinColumn(name = "host_id", nullable = true)
    private Member host;

    @Column(name = "title", length = 15, nullable = false)
    private String title;

    @Column(name = "password", length = 30)
    private String password;

    @Column(name = "max_player", nullable = false)
    private Integer maxPlayer;

    @Column(name = "max_game_round", nullable = false)
    private Integer maxGameRound;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 25)
    private RoomFormat format;

    @Column(name = "play_time", nullable = false)
    private Integer playTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoomStatus status;

    @ElementCollection
    @CollectionTable(
            name = "room_member",
            joinColumns = @JoinColumn(name = "room_id")
    )
    @Column(name = "member_id")
    private List<Long> memberIds = new ArrayList<>();

    public enum RoomStatus {
        WAITING, IN_PROGRESS, FINISHED
    }

    public enum RoomFormat {
        BOARD, GENERAL
    }

    @Builder
    public Room(Member host, String title, String password, Integer maxPlayer, Integer maxGameRound, RoomFormat format, Integer playTime, RoomStatus status) {
        this.host = host;
        this.title = title;
        this.password = password;
        this.maxPlayer = maxPlayer == null ? 10 : maxPlayer;
        this.maxGameRound = maxGameRound == null ? 20 : maxGameRound;
        this.format = format == null ? RoomFormat.GENERAL : format;
        this.playTime = playTime == null ? 0 : playTime;
        this.status = status == null ? RoomStatus.WAITING : status;
    }

    public void update(String title, String password, RoomFormat format){
        this.title = title;
        this.password = password;
        this.format = format;
    }
}
