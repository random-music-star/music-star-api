package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.common.util.GameUtil;
import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.game.domain.InGameManager;
import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.dto.song.SongInfo;
import com.curioussong.alsongdalsong.game.event.GameStatusEvent;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import com.curioussong.alsongdalsong.game.timer.GameTimerManager;
import com.curioussong.alsongdalsong.gameround.event.GameRoundLogEvent;
import com.curioussong.alsongdalsong.gamesession.event.GameSessionLogEvent;
import com.curioussong.alsongdalsong.member.service.MemberService;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class GeneralGameService extends AbstractGameStrategy {

    private final ApplicationEventPublisher applicationEventPublisher;

    public GeneralGameService(
            InGameManager inGameManager,
            GameTimerManager gameTimerManager,
            GameMessageSender gameMessageSender,
            RoomManager roomManager,
            MemberService memberService,
            RoomRepository roomRepository,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        super(inGameManager, gameTimerManager, gameMessageSender, roomManager, memberService, roomRepository);
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    protected boolean isGameEnd(Room room, int currentRound) {
        return currentRound == roomManager.getMaxGameRound(room.getId())+1;
    }

    public void handleRoundStart(String destination, Long channelId, Room room) {
        int currentRound = inGameManager.getCurrentRound(room.getId());
        GameMode gameMode = inGameManager.getCurrentRoundGameMode(room.getId());
        SongInfo currentRoundSong = inGameManager.getCurrentRoundSong(room.getId());

        int songPlayTime = currentRoundSong.getPlayTime();

        SongInfo secondSong = null;
        if (inGameManager.hasSecondSongInCurrentRound(room.getId())) {
            secondSong = inGameManager.getSecondSongForCurrentRound(room.getId());
            songPlayTime = Math.min(songPlayTime, secondSong.getPlayTime());
        }

        applicationEventPublisher.publishEvent(new GameRoundLogEvent(
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

        int finalSongPlayTime = songPlayTime;
        startRoundTimer(destination, currentRound, gameMode, currentRoundSong, secondSong, room, finalSongPlayTime, channelId);
    }

    private void startRoundTimer(String destination, int currentRound, GameMode gameMode, SongInfo currentRoundSong, SongInfo secondSong, Room room, int finalSongPlayTime, Long channelId) {
        gameTimerManager.handleRoundStart(destination, currentRound, gameMode, currentRoundSong, secondSong, room.getId(), () -> {
            // 카운트다운 완료 후 실행될 코드
            updateForNewRound(room);
            startSongPlayTimer(destination, finalSongPlayTime, room, channelId);
        });
    }

    private void startSongPlayTimer(String destination, int finalSongPlayTime, Room room, Long channelId) {
        gameTimerManager.scheduleSongPlayTime(
                destination,
                finalSongPlayTime,
                room.getId(),
                () -> triggerEndEvent(destination, channelId, room)
        );
    }

    private void updateForNewRound(Room room) {
        inGameManager.resetAnswered(room.getId());
        inGameManager.updateIsSongPlaying(room.getId());
    }

    public void triggerEndEvent(String destination, Long channelId, Room room) {
        log.debug("End event for channel {} in room {}", channelId, room.getId());
        if (room.getMembers().isEmpty() || inGameManager.getInGameInfo(room.getId()) == null) {
            log.info("방 {} 게임 종료 - 멤버가 없거나 게임 정보가 삭제되어 처리를 중단합니다.", room.getId());
            return;
        }
        try {
            inGameManager.updateIsSongPlaying(room.getId());

            boolean isWinnerExist = (inGameManager.getRoundWinner(room.getId()).get(room.getId()) != null);
            String winner = inGameManager.getRoundWinner(room.getId()).get(room.getId());

            if (isWinnerExist) {
                int winnerScore = inGameManager.getScore(room.getId()).get(winner);
                gameMessageSender.sendUserPosition(destination, winner, winnerScore);
            } else {
                sendGameResult(destination, room.getId(), null);
            }

            GameUtil.sleep(4000);
            applicationEventPublisher.publishEvent(new GameRoundLogEvent(
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

    @Override
    protected void publishGameEndEvents(Room room) {
        applicationEventPublisher.publishEvent(new GameStatusEvent(room, "WAITING"));
        applicationEventPublisher.publishEvent(new GameSessionLogEvent(room, GameSessionLogEvent.Type.END));
    }

    @Override
    protected void addScore(String userName, String roomId) {
                int scoreToAdd = 1;
        inGameManager.getScore(roomId).put(userName, inGameManager.getScore(roomId).get(userName) + scoreToAdd);
    }
}
