package com.curioussong.alsongdalsong.game.domain;

import com.curioussong.alsongdalsong.gameround.domain.GameRound;
import com.curioussong.alsongdalsong.member.domain.Member;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "game_chat")
@Builder
public class GameChat {
    @Id
    private String id;
    private Long memberId;
    private Long gameSessionId;
    private Long gameRoundId;
    private String message;
    private LocalDateTime chattedAt;
}