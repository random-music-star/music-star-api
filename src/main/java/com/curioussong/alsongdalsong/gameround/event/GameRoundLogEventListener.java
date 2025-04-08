package com.curioussong.alsongdalsong.gameround.event;

import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.game.domain.InGameManager;
import com.curioussong.alsongdalsong.game.dto.song.SongType;
import com.curioussong.alsongdalsong.gameround.domain.GameRound;
import com.curioussong.alsongdalsong.gameround.repository.GameRoundRepository;
import com.curioussong.alsongdalsong.gamesession.domain.GameSession;
import com.curioussong.alsongdalsong.gamesession.repository.GameSessionRepository;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.repository.MemberRepository;
import com.curioussong.alsongdalsong.song.domain.Song;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameRoundLogEventListener {

    private final GameRoundRepository gameRoundRepository;
    private final GameSessionRepository gameSessionRepository;
    private final MemberRepository memberRepository;
    private final InGameManager inGameManager;

    @Transactional
    @EventListener
    public void handleGameRoundLogEvent(GameRoundLogEvent event) {
        String roomId = event.room().getId();
        Long sessionId = inGameManager.getGameSessionId(roomId);

        if (event.type() == GameRoundLogEvent.Type.START) {
            handleRoundStartEvent(event, sessionId);
        } else if (event.type() == GameRoundLogEvent.Type.END) {
            handleRoundEndEvent(event, sessionId);
        }
    }

    private void handleRoundStartEvent(GameRoundLogEvent event, Long sessionId) {
        GameSession session = gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalStateException("GameSession not found"));

        GameRound.GameRoundBuilder gameRoundBuilder = GameRound.builder()
                .gameSession(session)
                .roundNumber(event.roundNumber())
                .gameMode(event.gameMode())
                .year(event.firstSong().getYear())
                .startTime(event.timestamp());

        if (event.firstSong().getType() == SongType.TTS_SONG) {
            // TTS 모드
            gameRoundBuilder.ttsSong(event.firstSong().toTtsSong());
        } else {
            // FULL 또는 DUAL 모드
            gameRoundBuilder
                    .song(event.firstSong().toSong());

            // DUAL 모드는 secondSong도 설정
            if (event.gameMode() == GameMode.DUAL && event.secondSong() != null) {
                gameRoundBuilder.secondSong(event.secondSong().toSong());
            }
        }
        GameRound round = gameRoundBuilder.build();

        gameRoundRepository.save(round);
        log.debug("GameRound START 기록 - session: {}, round: {}", sessionId, event.roundNumber());
    }

    private void handleRoundEndEvent(GameRoundLogEvent event, Long sessionId) {
        GameRound round = gameRoundRepository.findByGameSessionIdAndRoundNumber(sessionId, event.roundNumber())
                .orElseThrow(() -> new IllegalStateException("GameRound not found for END"));

        Member winner = event.winnerUsername() != null
                ? memberRepository.findByUsername(event.winnerUsername()).orElse(null)
                : null;

        round.finish(event.timestamp(), winner, event.submittedAnswer());

        log.debug("GameRound END 업데이트 - session: {}, round: {}, winner: {}",
                sessionId, event.roundNumber(), event.winnerUsername());
    }
}
