package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.chat.dto.ChatRequest;
import com.curioussong.alsongdalsong.game.domain.Game;
import com.curioussong.alsongdalsong.game.domain.Game.GameMode;
import com.curioussong.alsongdalsong.game.dto.quiz.QuizResponse;
import com.curioussong.alsongdalsong.game.dto.quiz.QuizResponseDTO;
import com.curioussong.alsongdalsong.game.dto.result.ResultResponse;
import com.curioussong.alsongdalsong.game.dto.result.ResultResponseDTO;
import com.curioussong.alsongdalsong.game.dto.roominfo.RoomInfoResponse;
import com.curioussong.alsongdalsong.game.dto.roominfo.RoomInfoResponseDTO;
import com.curioussong.alsongdalsong.game.dto.round.RoundResponse;
import com.curioussong.alsongdalsong.game.dto.round.RoundResponseDTO;
import com.curioussong.alsongdalsong.game.dto.timer.Response;
import com.curioussong.alsongdalsong.game.dto.timer.TimerResponse;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfo;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfoResponse;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfoResponseDTO;
import com.curioussong.alsongdalsong.game.repository.GameRepository;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.service.MemberService;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.service.RoomService;
import com.curioussong.alsongdalsong.roomgame.domain.RoomGame;
import com.curioussong.alsongdalsong.roomgame.service.RoomGameService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;
    private final MemberService memberService;
    private final GameRepository gameRepository;
    private final RoomGameService roomGameService;

    private Map<Long, Integer> roomAndRound = new HashMap<>(); // <roomId, round> 쌍으로 저장
    private Map<Long, Map<Integer,GameMode>> roundAndMode = new HashMap<>(); // <roomId, <round, FULL>> 쌍으로 저장
    private Map<Long, Map<Integer, String>> roundAndSong = new HashMap<>(); // <roomId, <round, songTitle>> 쌍으로 저장

    public void startGame(Long channelId, Long roomId) {
        initializeGameSetting(roomId);
        startRound(channelId, roomId);
    }

    public void startRound(Long channelId, Long roomId) {
        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);
        sendRoundInfo(destination, roomId);
        startCountdown(destination);
    }

    private void initializeGameSetting(Long roomId) {
        Map<Integer, GameMode> roundMap = new HashMap<>();
        Map<Integer, String> songMap = new HashMap<>();
        for (int i=1;i<=20;i++) { // 현재는 한 게임당 20라운드 하드코딩
            roundMap.put(i,GameMode.FULL);
            songMap.put(i, "톰보이");
        }
        roomAndRound.put(roomId, 1);
        roundAndMode.put(roomId, roundMap);
        roundAndSong.put(roomId, songMap);
    }

    public void sendRoundInfo(String destination, Long roomId) {
        RoomGame roomGame = roomGameService.getRoomGameByRoomId(roomId);
        Long gameId = roomGame.getGame().getId();
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new RuntimeException("gameId not found"));
        GameMode gameMode = game.getMode();
//        Random random = new Random();
//        GameMode selectedGameMode = gameModes.get(random.nextInt(gameModes.size()));
//        log.info("선택된 게임 모드: {}", selectedGameMode);

        messagingTemplate.convertAndSend(destination, RoundResponseDTO.builder()
                        .type("roundInfo")
                        .roundResponse(RoundResponse.builder()
                                .mode(gameMode)
                                .round(roomAndRound.get(roomId))
                                .build())
                .build());
    }

    public void startCountdown(String destination) {
        new Thread(() -> {
            try {
                for (int i = 3; i > 0; i--) {
                    sendCountdown(destination, i);
                    Thread.sleep(1000);
                }
                sendCountdown(destination, 0);
                sendQuizInfo(destination);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void sendQuizInfo(String destination) {
        messagingTemplate.convertAndSend(destination, QuizResponseDTO.builder()
                        .type("quizInfo")
                        .quizResponse(QuizResponse.builder()
                                .songUrl("https://www.youtube.com/watch?v=0wezH4MAncY")
                                .build())
                .build());
    }

    private void sendCountdown(String destination, int countdown) {
        TimerResponse timerResponse = TimerResponse.builder()
                .type("timer")
                .response(Response.builder()
                        .remainTime(countdown)
                        .build())
                .build();

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

    public boolean checkAnswer(ChatRequest chatRequest, Long roomId) {
        String message = chatRequest.getRequest().getMessage();
        int nowRound = roomAndRound.get(roomId);
        String nowAnswer = roundAndSong.get(roomId).get(nowRound);
        return message.equals(nowAnswer);
    }

    public void handleAnswer(String userName, Long channelId, Long roomId) {
        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);
        // 추후 DB에서 노래 문제 리스트를 Map
        messagingTemplate.convertAndSend(destination, ResultResponseDTO.builder()
                        .type("gameResult")
                        .response(ResultResponse.builder()
                                .winner(userName)
                                .songTitle("톰보이")
                                .singer("여자아이들")
                                .score(1)
                                .build())
                .build());
        roomAndRound.put(roomId, roomAndRound.get(roomId) + 1);
        startRound(channelId, roomId);
    }
}