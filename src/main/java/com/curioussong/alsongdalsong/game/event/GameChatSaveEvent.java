package com.curioussong.alsongdalsong.game.event;

import com.curioussong.alsongdalsong.gameround.domain.GameRound;
import com.curioussong.alsongdalsong.member.domain.Member;

import java.time.LocalDateTime;

public record GameChatSaveEvent (Long memberId, Long gameSessionId, Long gameRoundId, String message, LocalDateTime chattedAt){
}
