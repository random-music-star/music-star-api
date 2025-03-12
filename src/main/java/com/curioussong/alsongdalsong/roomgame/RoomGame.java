package com.curioussong.alsongdalsong.roomgame;

import com.curioussong.alsongdalsong.game.Game;
import com.curioussong.alsongdalsong.room.Room;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Builder
    public RoomGame(Game game, Room room) {
        this.game = game;
        this.room = room;
    }
}
