package com.curioussong.alsongdalsong.member.domain;

import com.curioussong.alsongdalsong.channel.domain.Channel;
import com.curioussong.alsongdalsong.room.domain.Room;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 20)
    private String username;

    @Column(name = "password", length = 30)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name="type", nullable=false)
    private MemberType type;

    public enum MemberType {
        USER, GUEST
    }

    @ManyToOne
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_channel_id")
    private Channel currentChannel;

    @Column(name="color_code", nullable = false, length = 7)
    private String colorCode;

    public void setRoom(Room room) {
        this.room = room;
    }

    @Builder
    public Member(String username, String password, MemberType type) {
        this.username = username;
        this.password = password;
        this.type = type;
    }

    public void enterChannel(Channel channel) {
        this.currentChannel = channel;
    }

    public void leaveChannel() {
        this.currentChannel = null;
    }

    public void updateColorCode(String colorCode) { this.colorCode = colorCode; }
}
