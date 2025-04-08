package com.curioussong.alsongdalsong.room.domain;

import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.channel.domain.Channel;
import com.curioussong.alsongdalsong.room.dto.RoomDTO;
import com.curioussong.alsongdalsong.roomyear.domain.RoomYear;
import com.github.f4b6a3.ulid.UlidCreator;
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
    @Column(length = 26)
    private String id;

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

    @Column(name = "room_number", nullable = false)
    private Long roomNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "room", fetch = FetchType.EAGER)
    private List<Member> members = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if(this.id==null) {
            this.id = UlidCreator.getUlid().toString();
        }
    }

    public enum RoomStatus {
        WAITING, IN_PROGRESS, FINISHED
    }

    public enum RoomFormat {
        BOARD, GENERAL
    }

    @Builder
    public Room(Member host, String title, String password, Integer maxPlayer, Integer maxGameRound, RoomFormat format, Integer playTime, RoomStatus status, Channel channel) {
        this.host = host;
        this.title = title;
        this.password = password;
        this.maxPlayer = maxPlayer;
        this.maxGameRound = maxGameRound;
        this.format = format;
        this.playTime = playTime == null ? 0 : playTime;
        this.status = status == null ? RoomStatus.WAITING : status;
        this.channel = channel;
    }

    public void update(String title, String password, Integer maxPlayer, Integer maxGameRound) {
        this.title = title;
        this.password = password;
        this.maxPlayer = maxPlayer;
        this.maxGameRound = maxGameRound;
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
                .roomNumber(this.roomNumber)
                .channelId(this.channel.getId())
                .build();
    }

    public void updateStatus(RoomStatus status) {
        if(isChangeableStatus()) {
            this.status = status;
        }
    }

    private boolean isChangeableStatus(){
        return this.status != RoomStatus.FINISHED;
    }

    public void addMember(Member member) {
        members.add(member);
        member.setRoom(this);
    }

    public void removeMember(Member member) {
        members.remove(member);
        member.setRoom(null);
    }

    public void updateHost(Member member) {
        this.host = member;
    }

    public void assignRoomNumber(Long roomNumber) {
        this.roomNumber = roomNumber;
    }
}
