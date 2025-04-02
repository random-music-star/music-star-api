package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.game.domain.InGameManager;
import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.dto.chat.ChatRequestDTO;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfo;
import com.curioussong.alsongdalsong.game.event.GameStatusEvent;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.service.MemberService;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import jakarta.persistence.EntityNotFoundException;
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

    private final RoomManager roomManager;
    private final InGameManager inGameManager;
    private final GameMessageSender gameMessageSender;
    private final GeneralGameService generalGameService;
    private final BoardGameService boardGameService;

    public void channelChatMessage(ChatRequestDTO chatRequestDTO, Long channelId) {
        String destination = String.format("/topic/channel/%d", channelId);

        gameMessageSender.sendChat(chatRequestDTO, destination);
    }

    public void roomChatMessage(ChatRequestDTO chatRequestDTO, Long channelId, String roomId) {
        String destination = String.format("/topic/channel/%d/room/%s", channelId, roomId);
        gameMessageSender.sendChat(chatRequestDTO, destination);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));

        if (room.getStatus() == Room.RoomStatus.WAITING) {
            return;
        }

        if (room.getFormat() == Room.RoomFormat.GENERAL) {
            // Skip 요청 처리
            if (".".equals(chatRequestDTO.getRequest().getMessage())) {
                generalGameService.incrementSkipCount(roomId, channelId, chatRequestDTO.getRequest().getSender());
            } else if (generalGameService.checkAnswer(chatRequestDTO, roomId)) {
                generalGameService.handleAnswer(chatRequestDTO.getRequest().getSender(), channelId, roomId);
            }
        } else if (room.getFormat() == Room.RoomFormat.BOARD) {
            if (".".equals(chatRequestDTO.getRequest().getMessage())) {
                boardGameService.incrementSkipCount(roomId, channelId, chatRequestDTO.getRequest().getSender());
            } else if (boardGameService.checkAnswer(chatRequestDTO, roomId)) {
                boardGameService.handleAnswer(chatRequestDTO.getRequest().getSender(), channelId, roomId);
            }
        }
    }

    //    Todo: 방에 입장한 유저만 레디 상태 변경이 가능하도록 변경 필요
    @Transactional
    public void toggleReady(String username, Long channelId, String roomId) {
        Map<Long, Boolean> roomReadyStatus = roomManager.getReadyStatus(roomId);
        Member member = memberService.getMemberByToken(username);
        boolean currentReady = Boolean.TRUE.equals(roomReadyStatus.getOrDefault(member.getId(), false));

        boolean newReady = !currentReady;
        log.debug("User {} ready status: {} -> {}", username, currentReady, newReady);

        Long memberId = memberService.getMemberByToken(username).getId();
        roomReadyStatus.put(memberId, !currentReady);

        log.debug("Room {} ready status map: {}", roomId, roomReadyStatus);

        String destination = String.format("/topic/channel/%d/room/%s", channelId, roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));


        List<UserInfo> userInfoList = roomManager.getUserInfos(room);
        boolean allReady = roomManager.isAllReady(room);
        gameMessageSender.sendUserInfo(destination, userInfoList, allReady);
    }

    public void updateSongYears(String roomId, List<Integer> selectedYears) {
        roomManager.setSelectedYears(roomId, selectedYears);
    }


    @Transactional
    public void startGame(Long channelId, String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        log.debug("Starting game in room {}", roomId);
        if (!roomManager.areAllPlayersReady(roomId)) {
            log.debug("Not all players are ready in room {}. Cancelling game start.", roomId);
            return;
        }
        log.debug("All players are ready, proceeding to start the game.");
        eventPublisher.publishEvent(new GameStatusEvent(room, "IN_PROGRESS"));

        String destination = String.format("/topic/channel/%d/room/%s", channelId, roomId);
        gameMessageSender.sendGameStart(destination);
        gameMessageSender.sendRoomInfo(destination, room, roomManager.getSelectedYears(roomId), roomManager.getGameModes(roomId));

        inGameManager.initializeGameSettings(room);
        if (room.getFormat() == Room.RoomFormat.GENERAL) {
            generalGameService.startRound(channelId, room, destination);
        } else {
            boardGameService.startRound(channelId, room, destination);
        }

        // TODO : readyStatusMap.remove(roomId);
    }
}