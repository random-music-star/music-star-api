package com.curioussong.alsongdalsong.roomyear.domain;

import com.curioussong.alsongdalsong.room.domain.Room;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "room_year")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "year", nullable = false, columnDefinition = "YEAR")
    private Integer year;

    @Builder
    public RoomYear(Room room, Integer year) {
        this.room = room;
        this.year = year;
    }
}

