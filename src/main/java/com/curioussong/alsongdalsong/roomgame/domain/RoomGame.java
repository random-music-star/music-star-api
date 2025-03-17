package com.curioussong.alsongdalsong.roomgame.domain;

import com.curioussong.alsongdalsong.game.domain.Game;
import com.curioussong.alsongdalsong.room.domain.Room;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "room_game")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Game game;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @Builder
    public RoomGame(Game game, Room room) {
        this.game = game;
        this.room = room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public void setGame(Game game) {
        this.game  = game;
    }
}
