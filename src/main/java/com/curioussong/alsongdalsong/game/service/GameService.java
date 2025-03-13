package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.game.domain.Game.GameMode;
import com.curioussong.alsongdalsong.game.dto.roominfo.RoomInfoResponse;
import com.curioussong.alsongdalsong.game.dto.roominfo.RoomInfoResponseDTO;
import com.curioussong.alsongdalsong.game.dto.timer.Response;
import com.curioussong.alsongdalsong.game.dto.timer.TimerResponse;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfo;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfoResponse;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfoResponseDTO;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.service.MemberService;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.service.RoomService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;
    private final MemberService memberService;

    public void startGame(String channelId, String roomId) {
        String destination = String.format("/topic/channel/%s/room/%s", channelId, roomId);
        startCountdown(destination);
    }

    public void startCountdown(String destination) {
        new Thread(() -> {
            try {
                for (int i = 3; i > 0; i--) {
                    sendCountdown(destination, i);
                    Thread.sleep(1000);
                }
                sendCountdown(destination, 0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void sendCountdown(String destination, int countdown) {
        TimerResponse timerResponse = TimerResponse.builder()
                .type("timer")
                .response(Response.builder()
                        .remainTime(countdown)
                        .build())
                .build();

        log.info("Sending countdown response: {}", timerResponse);
        messagingTemplate.convertAndSend(destination, timerResponse);
    }

    @Transactional
    public void sendRoomInfoAndUserInfoToSubscriber(Long channelId, Long roomId) {
        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);
        Room room = roomService.findRoomById(roomId);
        sendRoomInfoToSubscriber(destination, room);
        sendUserInfoToSubscriber(destination, room);
    }

    private void sendRoomInfoToSubscriber(String destination, Room room) {
        // 임시 하드코딩
        List<GameMode> gameModes = new ArrayList<>();
        gameModes.add(GameMode.FULL);
        List<Integer> selectedYear = new ArrayList<>();
        selectedYear.add(2020);
        selectedYear.add(2021);
        selectedYear.add(2022);
        selectedYear.add(2023);
        selectedYear.add(2024);

        messagingTemplate.convertAndSend(destination, RoomInfoResponseDTO.builder()
                .type("roomInfo")
                .response(RoomInfoResponse.builder()
                        .roomTitle(room.getTitle())
                        .password(room.getPassword())
                        .maxPlayer(room.getMaxPlayer())
                        .maxGameRound(room.getMaxGameRound())
                        .format(room.getFormat())
                        .status(room.getStatus())
                        .mode(gameModes)
                        .selectedYear(selectedYear)
                        .build())
                .build());
    }

    private void sendUserInfoToSubscriber(String destination, Room room) {
        List<Long> memberIds = room.getMemberIds();
        List<UserInfo> userInfoList = new ArrayList<>();
        for (Long memberId : memberIds) {
            Member member = memberService.getMemberById(memberId);
            UserInfo userInfo = new UserInfo(member.getUsername(), false);
            userInfoList.add(userInfo);
        }

        messagingTemplate.convertAndSend(destination, UserInfoResponseDTO
                .builder()
                .type("userInfo")
                .response(UserInfoResponse.builder()
                        .userInfoList(userInfoList)
                        .build())
                .build());
    }
}
