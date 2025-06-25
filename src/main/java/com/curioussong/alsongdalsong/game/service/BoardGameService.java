package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.common.util.GameUtil;
import com.curioussong.alsongdalsong.game.board.BoardEventHandler;
import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.game.domain.InGameManager;
import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.dto.board.BoardEventResponseDTO;
import com.curioussong.alsongdalsong.game.dto.song.SongInfo;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import com.curioussong.alsongdalsong.game.timer.GameTimerManager;
import com.curioussong.alsongdalsong.gameround.event.GameRoundLogEvent;
import com.curioussong.alsongdalsong.gamesession.event.GameSessionLogEvent;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.event.MemberLocationEvent;
import com.curioussong.alsongdalsong.member.service.MemberService;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class BoardGameService extends AbstractGameStrategy {

    private final RoomRepository roomRepository;

    private final BoardEventHandler boardEventHandler;
    private final ApplicationEventPublisher eventPublisher;

    private static final int WINNING_SCORE = 20;
    private static final int BEFORE_MOVE_DELAY_MS = 500;
    private static final int MOVE_DELAY_MS = 300;

    public BoardGameService(
            InGameManager inGameManager,
            GameTimerManager gameTimerManager,
            GameMessageSender gameMessageSender,
            RoomManager roomManager,
            MemberService memberService,
            RoomRepository roomRepository,
            BoardEventHandler boardEventHandler,
            ApplicationEventPublisher eventPublisher
    ) {
        super(inGameManager, gameTimerManager, gameMessageSender, roomManager, memberService, roomRepository);
        this.roomRepository = roomRepository;
        this.boardEventHandler = boardEventHandler;
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected boolean isGameEnd(Room room, int currentRound) {
        return currentRound == roomManager.getMaxGameRound(room.getId()) + 1
                || inGameManager.getScore(room.getId()).values().stream().anyMatch(score -> score >= 20);
    }

    public void handleRoundStart(String destination, Long channelId, Room room) {
        String roomId = room.getId();
        int currentRound = inGameManager.getCurrentRound(roomId);
        GameMode gameMode = inGameManager.getCurrentRoundGameMode(roomId);

        SongInfo currentRoundSong = inGameManager.getCurrentRoundSong(roomId);
        SongInfo secondSong = getSecondSongIfExists(roomId);
        int songPlayTime = calculateSongPlayTime(currentRoundSong, secondSong);

        eventPublisher.publishEvent(new GameRoundLogEvent(
                room,
                GameRoundLogEvent.Type.START,
                currentRound,
                gameMode,
                currentRoundSong,
                secondSong,
                LocalDateTime.now(),
                null,
                null
        ));

        gameTimerManager.handleRoundStart(destination, currentRound, gameMode, currentRoundSong, secondSong, roomId, () -> {
            // 카운트다운 완료 후 실행될 코드
            inGameManager.resetAnswered(roomId);

            gameTimerManager.scheduleSongPlayTime(
                    destination,
                    songPlayTime,
                    roomId,
                    () -> triggerEndEvent(destination, channelId, room)
            );
            inGameManager.updateIsSongPlaying(roomId);
        });
    }

    private SongInfo getSecondSongIfExists(String roomId) {
        if (inGameManager.hasSecondSongInCurrentRound(roomId)) {
            return inGameManager.getSecondSongForCurrentRound(roomId);
        }
        return null;
    }

    private int calculateSongPlayTime(SongInfo song1, SongInfo song2) {
        if (song2 == null) {
            return song1.getPlayTime();
        }
        int minPlayTime = Math.min(song1.getPlayTime(), song2.getPlayTime());
        return Math.max(minPlayTime - 30, 0);
    }

    public void triggerEndEvent(String destination, Long channelId, Room room) {
        String roomId = room.getId();
        log.debug("End event for channel {} in room {}", channelId, roomId);

        if (room.getMembers().isEmpty() || inGameManager.getInGameInfo(room.getId()) == null) {
            log.debug("게임 정보가 이미 정리되어 이벤트 처리를 중단합니다.");
            return;
        }
        try {
            inGameManager.updateIsSongPlaying(room.getId());

            String winner = inGameManager.getRoundWinner(room.getId()).get(room.getId());
            boolean isWinnerExist = winner != null;

            sendMessageForBoardEvent(destination, roomId, winner, isWinnerExist);

            eventPublisher.publishEvent(new GameRoundLogEvent(
                    room,
                    GameRoundLogEvent.Type.END,
                    inGameManager.getCurrentRound(room.getId()),
                    null,
                    null,
                    null,
                    LocalDateTime.now(),
                    isWinnerExist ? winner : null,
                    inGameManager.getSubmittedAnswer(room.getId())
            ));
            inGameManager.clearSubmittedAnswer(room.getId());
            inGameManager.nextRound(room.getId());
            startRound(channelId, room, destination);
        } catch (NullPointerException e) {
            log.debug("방 {} 게임 정보가 이미 정리되었거나 멤버가 없어 이벤트 처리를 중단합니다. {}", room.getId(), e.getMessage());
        }
    }

    private void sendMessageForBoardEvent(String destination, String roomId, String winner, boolean isWinnerExist) {
        if (isWinnerExist) {
            gameMessageSender.sendNextMessage(destination, winner, inGameManager.getUserMovement(roomId).get(winner));
            GameUtil.sleep(1000);
            gameMessageSender.sendDiceMessage(destination);
            handleSendingPositionMessage(destination, roomId, winner);
        } else { // 정답자 없이 스킵된 경우 바로 다음 라운드 시작
            sendGameResult(destination, roomId, winner);
            GameUtil.sleep(3000);
        }
    }

    private void handleSendingPositionMessage(String destination, String roomId, String currentRoundWinner) {
        Map<String, Integer> userMovement = inGameManager.getUserMovement(roomId);
        Map<String, Integer> userScores = inGameManager.getScore(roomId);

        gameMessageSender.sendUserPosition(destination, currentRoundWinner, userScores.get(currentRoundWinner));
        GameUtil.sleep(BEFORE_MOVE_DELAY_MS);
        for (int movement = 1;movement <= userMovement.get(currentRoundWinner); movement++) {
            userScores.put(currentRoundWinner, userScores.get(currentRoundWinner) + 1);
            gameMessageSender.sendUserPosition(destination, currentRoundWinner, userScores.get(currentRoundWinner));

            if (userScores.get(currentRoundWinner) >= WINNING_SCORE) { // 목표 지점 도달 시
                return;
            }

            GameUtil.sleep(MOVE_DELAY_MS);
        }

        // 이벤트 생성 및 처리
        int playerCount = userScores.size();
        BoardEventResponseDTO eventResponse = boardEventHandler.generateEvent(currentRoundWinner, playerCount, roomId);
        boardEventHandler.handleEvent(destination, roomId, eventResponse);
    }

    @Override
    protected void addScore(String userName, String roomId) {
        int scoreToAdd = calculateScore();
        inGameManager.getUserMovement(roomId).put(userName, scoreToAdd); // 정답자 이동 예정 횟수 갱신
        log.debug("score to add: {}", scoreToAdd);
    }

    private int calculateScore() {
        SecureRandom random = new SecureRandom();
        return random.nextInt(1, 4);
    }

    @Override
    protected void publishGameEndEvents(Room room) {
        eventPublisher.publishEvent(new GameSessionLogEvent(room, GameSessionLogEvent.Type.END));
        roomRepository.save(room);
        Long channelId = room.getChannel().getId();
        for (Member member : room.getMembers()) {
            eventPublisher.publishEvent(new MemberLocationEvent(channelId, member));
        }
    }
}
