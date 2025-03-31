package com.curioussong.alsongdalsong.channel.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "channel")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "max_users", nullable = false)
    private Integer maxUsers;

    @Builder
    public Channel(Integer maxUsers) {
        this.maxUsers = maxUsers != null ? maxUsers : 500;
    }
}
