package com.curioussong.alsongdalsong.gameround.event;

import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.game.dto.song.SongInfo;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.song.domain.Song;
import com.curioussong.alsongdalsong.ttssong.domain.TtsSong;

import java.time.LocalDateTime;

public record GameRoundLogEvent(
        Room room,
        Type type,
        int roundNumber,
        GameMode gameMode,
        SongInfo firstSong,
        SongInfo secondSong,
        LocalDateTime timestamp,
        String winnerUsername,
        String submittedAnswer
) {
    public enum Type {
        START, END
    }
}
