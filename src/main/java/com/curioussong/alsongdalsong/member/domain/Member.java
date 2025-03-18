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

    public void setRoom(Room room) {
        this.room = room;
    }

    @Builder
    public Member(String username, String password, MemberType type) {
        this.username = username;
        this.password = password;
        this.type = type;
    }
}
