package com.curioussong.alsongdalsong.game.messaging;

import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.game.dto.board.BoardEventResponseDTO;
import com.curioussong.alsongdalsong.game.dto.board.EventEndResponseDTO;
import com.curioussong.alsongdalsong.game.dto.board.EventTriggerResponse;
import com.curioussong.alsongdalsong.game.dto.board.EventTriggerResponseDTO;
import com.curioussong.alsongdalsong.game.dto.chat.ChatRequestDTO;
import com.curioussong.alsongdalsong.game.dto.chat.ChatResponse;
import com.curioussong.alsongdalsong.game.dto.chat.ChatResponseDTO;
import com.curioussong.alsongdalsong.game.dto.gameend.GameEndResponse;
import com.curioussong.alsongdalsong.game.dto.gameend.GameEndResponseDTO;
import com.curioussong.alsongdalsong.game.dto.hint.HintResponse;
import com.curioussong.alsongdalsong.game.dto.hint.HintResponseDTO;
import com.curioussong.alsongdalsong.game.dto.move.MoveResponse;
import com.curioussong.alsongdalsong.game.dto.move.MoveResponseDTO;
import com.curioussong.alsongdalsong.game.dto.next.NextResponse;
import com.curioussong.alsongdalsong.game.dto.next.NextResponseDTO;
import com.curioussong.alsongdalsong.game.dto.quiz.QuizResponse;
import com.curioussong.alsongdalsong.game.dto.quiz.QuizResponseDTO;
import com.curioussong.alsongdalsong.game.dto.result.ResultResponse;
import com.curioussong.alsongdalsong.game.dto.result.ResultResponseDTO;
import com.curioussong.alsongdalsong.game.dto.roominfo.RoomInfoResponse;
import com.curioussong.alsongdalsong.game.dto.roominfo.RoomInfoResponseDTO;
import com.curioussong.alsongdalsong.game.dto.round.RoundResponse;
import com.curioussong.alsongdalsong.game.dto.round.RoundResponseDTO;
import com.curioussong.alsongdalsong.game.dto.skip.SkipResponse;
import com.curioussong.alsongdalsong.game.dto.skip.SkipResponseDTO;
import com.curioussong.alsongdalsong.game.dto.timer.Response;
import com.curioussong.alsongdalsong.game.dto.timer.TimerResponse;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfo;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfoResponse;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfoResponseDTO;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.dto.RefuseEnterResponse;
import com.curioussong.alsongdalsong.room.dto.RefuseEnterResponseDTO;
import com.curioussong.alsongdalsong.song.domain.Song;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameMessageSender {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendChat(ChatRequestDTO chatRequestDTO, String destination) {
        ChatResponse chatResponse = ChatResponse.builder()
                .sender(chatRequestDTO.getRequest().getSender())
                .messageType("default")
                .message(chatRequestDTO.getRequest().getMessage())
                .build();

        ChatResponseDTO chatResponseDTO = ChatResponseDTO.builder()
                .type(chatRequestDTO.getType())
                .response(chatResponse)
                .build();

        messagingTemplate.convertAndSend(destination, chatResponseDTO);
    }

    public void sendRoundInfoAndQuizInfo(String destination, int currentRound, GameMode gameMode, Song currentRoundSong) {
        sendRoundInfo(destination, currentRound, gameMode);
        sendQuizInfo(destination, currentRoundSong.getUrl());
    }

    public void sendRoundInfo(String destination, int currentRound, GameMode gameMode) {
        log.debug("sendRoundInfo 호출됨 - destination: {}, round: {}", destination, currentRound);

        messagingTemplate.convertAndSend(destination, RoundResponseDTO.builder()
                .type("roundInfo")
                .response(RoundResponse.builder()
                        .mode(gameMode)
                        .round(currentRound)
                        .build())
                .build());

        log.debug("sendRoundInfo 메시지 전송 완료 - round: {}", currentRound);
    }

    public void sendCountdown(String destination, int countdown) {
        TimerResponse timerResponse = TimerResponse.builder()
                .type("timer")
                .response(Response.builder()
                        .remainTime(countdown)
                        .build())
                .build();

        messagingTemplate.convertAndSend(destination, timerResponse);
    }

    public void sendQuizInfo(String destination, String songUrl) {
        log.info("퀴즈 정보 전송");
        messagingTemplate.convertAndSend(destination, QuizResponseDTO.builder()
                .type("quizInfo")
                .response(QuizResponse.builder()
                        .songUrl(songUrl)
                        .build())
                .build());
    }

    public void sendHint(String destination, String title, String singer) {
        HintResponse.HintResponseBuilder builder = HintResponse.builder().title(title);
        if (singer != null) {
            builder.singer(singer);
        }

        messagingTemplate.convertAndSend(destination, HintResponseDTO.builder()
                .type("hint")
                .response(builder.build())
                .build());
    }

    public void sendGameResult(String destination, String userName, Song song) {
        String koreanTitle = song.getKorTitle();
        String englishTitle = song.getEngTitle();
        StringBuilder title = new StringBuilder();
        if (englishTitle.isBlank()) {
            title.append(koreanTitle);
        } else {
            if (englishTitle.matches(".*[a-zA-Z].*")) {
                title.append(englishTitle);
            }
        }

        messagingTemplate.convertAndSend(destination, ResultResponseDTO.builder()
                .type("gameResult")
                .response(ResultResponse.builder()
                        .winner(userName)
                        .songTitle(title.toString())
                        .singer(song.getArtist())
                        .score(1)
                        .build())
                .build());
    }

    public void sendSkipInfo(String destination, int skipCount) {
        messagingTemplate.convertAndSend(destination, SkipResponseDTO.builder()
                .type("skip")
                .response(SkipResponse.builder()
                        .skipPerson(skipCount)
                        .build())
                .build());
    }

    public void sendUserPosition(String destination, String userName, int position) {
        messagingTemplate.convertAndSend(destination, MoveResponseDTO.builder()
                .type("move")
                .response(MoveResponse.builder()
                        .username(userName)
                        .position(position)
                        .build())
                .build());
    }

    public void sendNextMessage(String destination, String username, Integer totalMovement) {
        messagingTemplate.convertAndSend(destination, NextResponseDTO.builder()
                .type("next")
                .response(NextResponse.builder()
                        .username(username)
                        .totalMovement(totalMovement)
                        .build())
                .build());
    }

    public void sendGameEndMessage(String destination, String finalWinner) {
        messagingTemplate.convertAndSend(destination, GameEndResponseDTO.builder()
                .type("gameEnd")
                .response(GameEndResponse.builder()
                        .winner(finalWinner)
                        .build())
                .build());
    }

    public void sendRoomInfo(String destination, Room room, List<Integer> selectedYears, List<GameMode> gameModes) {
        messagingTemplate.convertAndSend(destination, RoomInfoResponseDTO.builder()
                .type("roomInfo")
                .response(RoomInfoResponse.builder()
                        .roomTitle(room.getTitle())
                        .maxPlayer(room.getMaxPlayer())
                        .maxGameRound(room.getMaxGameRound())
                        .hasPassword(room.getPassword() != null && !room.getPassword().isEmpty())
                        .format(room.getFormat())
                        .status(room.getStatus())
                        .mode(gameModes)
                        .selectedYear(selectedYears)
                        .build())
                .build());
    }

    public void sendUserInfo(String destination, List<UserInfo> userInfoList, boolean allReady) {
        messagingTemplate.convertAndSend(destination, UserInfoResponseDTO
                .builder()
                .type("userInfo")
                .response(UserInfoResponse.builder()
                        .userInfoList(userInfoList)
                        .allReady(allReady)
                        .build())
                .build());
    }

    public void sendBoardEventMessage(String destination, BoardEventResponseDTO eventResponseDTO) {
        messagingTemplate.convertAndSend(destination, eventResponseDTO);
    }

    public void sendEventTrigger(String destination, String trigger) {
        EventTriggerResponse triggerResponse = EventTriggerResponse.builder()
                .trigger(trigger)
                .build();

        EventTriggerResponseDTO responseDTO = EventTriggerResponseDTO.builder()
                .response(triggerResponse)
                .build();

        messagingTemplate.convertAndSend(destination, responseDTO);
        log.debug("Event trigger message sent for user: {}", trigger);
    }

    public void sendEventEnd(String destination) {
        EventEndResponseDTO responseDTO = EventEndResponseDTO.builder().build();
        messagingTemplate.convertAndSend(destination, responseDTO);
        log.debug("Event end message sent");
    }

    public void sendRefuseMessage(String destination, String userName) {
        messagingTemplate.convertAndSend(destination, RefuseEnterResponseDTO.builder()
                .type("refuseEnter")
                .response(RefuseEnterResponse.builder()
                        .refusedUser(userName)
                        .build())
                .build());
    }
}
