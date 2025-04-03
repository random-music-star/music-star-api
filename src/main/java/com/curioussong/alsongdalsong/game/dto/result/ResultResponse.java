package com.curioussong.alsongdalsong.game.dto.result;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class ResultResponse {
    private String winner;
    private String songTitle;
    private String singer;
    private Integer score;
    private String songTitle2;
    private String singer2;

    public void assignSecondSong(String songTitle2, String artist) {
        this.songTitle2 = songTitle2;
        this.singer2 = artist;
    }
}
