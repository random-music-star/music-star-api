package com.curioussong.alsongdalsong.gamesession.event;

import com.curioussong.alsongdalsong.gamesession.domain.GameSession;
import com.curioussong.alsongdalsong.gamesession.repository.GameSessionRepository;
import com.curioussong.alsongdalsong.gamesessionmode.domain.GameSessionMode;
import com.curioussong.alsongdalsong.gamesessionmode.repository.GameSessionModeRepository;
import com.curioussong.alsongdalsong.gamesessionyear.domain.GameSessionYear;
import com.curioussong.alsongdalsong.gamesessionyear.repository.GameSessionYearRepository;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.roomgame.domain.RoomGame;
import com.curioussong.alsongdalsong.roomgame.repository.RoomGameRepository;
import com.curioussong.alsongdalsong.roomyear.domain.RoomYear;
import com.curioussong.alsongdalsong.roomyear.repository.RoomYearRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameSessionLogEventListener {

    private final GameSessionRepository gameSessionRepository;
    private final GameSessionModeRepository gameSessionModeRepository;
    private final GameSessionYearRepository gameSessionYearRepository;
    private final RoomGameRepository roomGameRepository;
    private final RoomYearRepository roomYearRepository;

    @EventListener
    @Transactional
    public void handleGameSessionLogEvent(GameSessionLogEvent event) {
        Room room = event.room();
        GameSessionLogEvent.Type type = event.type();

        log.debug("GameSessionLogEvent 수신 - roomId: {}, type: {}", room.getId(), type);

        try {
            if (type == GameSessionLogEvent.Type.START) {
                createGameSession(room);
            } else if (type == GameSessionLogEvent.Type.END) {
                finishGameSession(room);
            }
        } catch (Exception e) {
            log.error("게임 로그 저장 중 오류 발생", e);
        }
    }

    private void createGameSession(Room room) {
        log.debug("게임 세션 생성 시작 - roomId: {}", room.getId());

        GameSession gameSession = GameSession.builder()
                .roomId(room.getId())
                .title(room.getTitle())
                .maxPlayer(room.getMaxPlayer())
                .maxGameRound(room.getMaxGameRound())
                .format(room.getFormat())
                .password(room.getPassword())
                .startTime(LocalDateTime.now())
                .initialParticipantCount(room.getMembers().size())
                .host(room.getHost())
                .build();

        gameSessionRepository.save(gameSession);
        log.debug("게임 세션 저장 완료 - sessionId: {}", gameSession.getId());

        saveGameSessionModes(room, gameSession);
        saveGameSessionYears(room, gameSession);
        log.debug("게임 세션 생성 완료");
    }

    private void saveGameSessionModes(Room room, GameSession gameSession) {
        List<RoomGame> roomGames = roomGameRepository.findByRoomId(room.getId());
        for (RoomGame roomGame : roomGames) {
            GameSessionMode sessionMode = GameSessionMode.builder()
                    .gameSession(gameSession)
                    .game(roomGame.getGame())
                    .build();
            gameSessionModeRepository.save(sessionMode);
        }
        log.debug("게임 세션 모드 저장 완료 - 총 {}개", roomGames.size());
    }

    private void saveGameSessionYears(Room room, GameSession gameSession) {
        List<RoomYear> roomYears = roomYearRepository.findByRoomId(room.getId());
        for (RoomYear roomYear : roomYears) {
            GameSessionYear sessionYear = GameSessionYear.builder()
                    .gameSession(gameSession)
                    .year(roomYear.getYear())
                    .build();
            gameSessionYearRepository.save(sessionYear);
        }
        log.debug("게임 세션 연도 저장 완료 - 총 {}개", roomYears.size());
    }

    private void finishGameSession(Room room) {
        log.debug("게임 세션 종료 시작 - roomId: {}", room.getId());

        GameSession gameSession = gameSessionRepository.findTopByRoomIdOrderByStartTimeDesc(room.getId())
                .orElse(null);

        if (gameSession == null) {
            log.warn("종료할 게임 세션을 찾을 수 없습니다 - roomId: {}", room.getId());
            return;
        }

        if (gameSession.getEndTime() != null) {
            log.debug("이미 종료된 게임 세션입니다 - sessionId: {}", gameSession.getId());
            return;
        }

        gameSession.finishSession(room.getMembers().size());
        log.debug("게임 세션 종료 완료 - sessionId: {}", gameSession.getId());
    }
}
