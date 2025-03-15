package com.curioussong.alsongdalsong.member.domain;

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

    @Column(name = "password", nullable = true, length = 30)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name="type", nullable=false)
    private MemberType type;

    @ManyToOne
    private Room room;

    public void setRoom(Room room) {
        this.room = room;
    }

    public enum MemberType {
        USER, GUEST
    }

    @Builder
    public Member(String username, MemberType type) {
        this.username = username;
        this.type = type;
    }
}
