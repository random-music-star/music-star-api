package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.common.error.stomperror.StompError;
import com.curioussong.alsongdalsong.common.error.stomperror.StompException;
import com.curioussong.alsongdalsong.common.util.Destination;
import com.curioussong.alsongdalsong.game.domain.InGameManager;
import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfo;
import com.curioussong.alsongdalsong.game.event.GameStatusEvent;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import com.curioussong.alsongdalsong.gameround.repository.GameRoundRepository;
import com.curioussong.alsongdalsong.gamesession.event.GameSessionLogEvent;
import com.curioussong.alsongdalsong.gamesession.repository.GameSessionRepository;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.service.MemberService;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final MemberService memberService;
    private final RoomRepository roomRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final GameStrategyFactory strategyFactory;

    private final RoomManager roomManager;
    private final InGameManager inGameManager;
    private final GameMessageSender gameMessageSender;
    private final GameRoundRepository gameRoundRepository;
    private final GameSessionRepository gameSessionRepository;

    //    Todo: 방에 입장한 유저만 레디 상태 변경이 가능하도록 변경 필요
    @Transactional
    public void toggleReady(String username, Long channelId, String roomId) {
        Member member = memberService.getMemberByToken(username);
        Long memberId = member.getId();

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new StompException(StompError.ROOM_NOT_FOUND));

        Map<Long, Boolean> roomReadyStatus = roomManager.getReadyStatus(roomId);
        boolean currentReady = Boolean.TRUE.equals(roomReadyStatus.getOrDefault(memberId, false));
        boolean newReady = !currentReady;

        roomManager.updateReadyStatus(roomId, memberId, newReady);
        log.debug("User {} ready status: {} -> {}", username, currentReady, newReady);
        log.debug("Room {} ready status map: {}", roomId, roomReadyStatus);


        sendUserInfo(room, channelId);
    }

    private void sendUserInfo(Room room, Long channelId) {
        List<UserInfo> userInfoList = roomManager.getUserInfos(room);
        boolean allReady = roomManager.isAllReady(room);
        String destination = Destination.room(channelId, room.getId());
        gameMessageSender.sendUserInfo(destination, userInfoList, allReady);
    }

    @Transactional
    public void startGame(Long channelId, String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new StompException(StompError.ROOM_NOT_FOUND));
        log.debug("Starting game in room {}", roomId);

        if (isNotReadyToStart(room)) {
            log.debug("Not all players are ready in room {}. Cancelling game start.", roomId);
            return;
        }
        log.debug("All players are ready, proceeding to start the game.");

        publishStartEvent(room);

        String destination = Destination.room(channelId, roomId);
        sendMessagesForStart(destination, room);

        inGameManager.initializeGameSettings(room);

        Room.RoomFormat roomFormat = room.getFormat();
        GameStrategy strategy = strategyFactory.getStrategy(roomFormat);
        strategy.startRound(channelId, room, destination);
    }

    private boolean isNotReadyToStart(Room room) {
        return room.getFormat() == Room.RoomFormat.BOARD && !roomManager.areAllPlayersReady(room.getId());
    }

    private void publishStartEvent(Room room) {
        eventPublisher.publishEvent(new GameStatusEvent(room, "IN_PROGRESS"));
        eventPublisher.publishEvent(new GameSessionLogEvent(room, GameSessionLogEvent.Type.START));
    }

    private void sendMessagesForStart(String destination, Room room) {
        gameMessageSender.sendGameStart(destination);
        gameMessageSender.sendRoomInfo(
                destination,
                room,
                roomManager.getSelectedYears(room.getId()),
                roomManager.getGameModes(room.getId())
        );
    }
}