package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.common.error.stomperror.StompError;
import com.curioussong.alsongdalsong.common.error.stomperror.StompException;
import com.curioussong.alsongdalsong.common.util.BadWordFilter;
import com.curioussong.alsongdalsong.game.domain.InGameManager;
import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.dto.chat.ChatRequestDTO;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfo;
import com.curioussong.alsongdalsong.game.event.GameChatSaveEvent;
import com.curioussong.alsongdalsong.game.event.GameStatusEvent;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import com.curioussong.alsongdalsong.game.timer.GameTimerManager;
import com.curioussong.alsongdalsong.gameround.domain.GameRound;
import com.curioussong.alsongdalsong.gameround.repository.GameRoundRepository;
import com.curioussong.alsongdalsong.gamesession.domain.GameSession;
import com.curioussong.alsongdalsong.gamesession.event.GameSessionLogEvent;
import com.curioussong.alsongdalsong.gamesession.repository.GameSessionRepository;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.service.MemberService;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import com.curioussong.alsongdalsong.common.util.Destination;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
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
    private final GeneralGameService generalGameService;
    private final BoardGameService boardGameService;
    private final GameRoundRepository gameRoundRepository;
    private final GameSessionRepository gameSessionRepository;
    private final GameTimerManager gameTimerManager;

    public void roomChatMessage(ChatRequestDTO chatRequestDTO, Long channelId, String roomId) {
        String destination = Destination.room(channelId, roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new StompException(StompError.ROOM_NOT_FOUND));

        // 대기 중인 방에서는 채팅 메시지 필터링 수행
        if (room.getStatus() == Room.RoomStatus.WAITING) {
            filterMessage(chatRequestDTO);
            gameMessageSender.sendChat(chatRequestDTO, destination);
            return;
        }

        // 추후 mongodb 연결 시 주석 해제
//        if (room.getStatus() == Room.RoomStatus.IN_PROGRESS) {
//            saveInGameChat(chatRequestDTO, roomId);
//        }

        // 사용자들에게 채팅 메시지 전송
        gameMessageSender.sendChat(chatRequestDTO, destination);

        String message = chatRequestDTO.getRequest().getMessage();
        String sender = chatRequestDTO.getRequest().getSender();
        Room.RoomFormat roomFormat = room.getFormat();

        // 방에 따라 다르게 처리
        GameStrategy strategy = strategyFactory.getStrategy(roomFormat);
        if (isSkipChat(message)) {
            strategy.incrementSkipCount(roomId, channelId, sender);
        } else if (strategy.checkAnswer(chatRequestDTO, roomId)) {
            strategy.handleAnswer(sender, channelId, roomId);
        }
    }

    private void filterMessage(ChatRequestDTO chatRequestDTO) {
        String filteredMessage = BadWordFilter.filter(chatRequestDTO.getRequest().getMessage());
        chatRequestDTO.getRequest().setMessage(filteredMessage);
    }

    private void saveInGameChat(ChatRequestDTO chatRequestDTO, String roomId) {
        Member member = memberService.getMemberByToken(chatRequestDTO.getRequest().getSender());
        GameSession gameSession = gameSessionRepository.findTopByRoomIdOrderByStartTimeDesc(roomId)
                .orElseThrow(() -> new StompException(StompError.GAME_SESSION_NOT_FOUND));
        int currentRound = inGameManager.getCurrentRound(roomId);
        GameRound round = gameRoundRepository.findByGameSessionIdAndRoundNumber(gameSession.getId(), currentRound)
                .orElseThrow(() -> new StompException(StompError.GAME_ROUND_NOT_FOUND));
        eventPublisher.publishEvent(new GameChatSaveEvent(member.getId(), gameSession.getId(), round.getId(), chatRequestDTO.getRequest().getMessage(), LocalDateTime.now()));
    }

    private boolean isSkipChat(String message) {
        return ".".equals(message);
    }

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