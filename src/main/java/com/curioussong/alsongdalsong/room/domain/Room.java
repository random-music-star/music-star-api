package com.curioussong.alsongdalsong.room.domain;

import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.room.dto.RoomDTO;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "room")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
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

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "room", fetch = FetchType.EAGER)
    private List<Member> members = new ArrayList<>();


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

    public RoomDTO toDto(){
        return RoomDTO.builder()
                .id(this.id)
                .title(this.title)
                .hostName(this.host.getUsername())
                .format(this.format.name())
                .maxPlayer(this.maxPlayer)
                .currentPlayers(this.members.size())
                .maxGameRound(this.maxGameRound)
                .playTime(this.playTime)
                .hasPassword(this.password != null && !this.password.isEmpty())
                .status(this.status.name())
                .build();
    }

    public void updateStatus(RoomStatus status) {
        this.status = status;
    }

    public void addMember(Member member) {
        members.add(member);
        member.setRoom(this);
    }

    public void removeMember(Member member) {
        members.remove(member);
        member.setRoom(null);
    }
}
