package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.chat.dto.ChatRequest;
import com.curioussong.alsongdalsong.game.domain.Game;
import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.game.domain.RoomInfo;
import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.dto.gameend.GameEndResponse;
import com.curioussong.alsongdalsong.game.dto.gameend.GameEndResponseDTO;
import com.curioussong.alsongdalsong.game.dto.hint.HintResponse;
import com.curioussong.alsongdalsong.game.dto.hint.HintResponseDTO;
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
import com.curioussong.alsongdalsong.game.repository.GameRepository;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.service.MemberService;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.game.event.GameStatusEvent;
import com.curioussong.alsongdalsong.room.event.UserJoinedEvent;
import com.curioussong.alsongdalsong.room.domain.Room.RoomStatus;
import com.curioussong.alsongdalsong.room.event.UserLeavedEvent;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import com.curioussong.alsongdalsong.roomgame.domain.RoomGame;
import com.curioussong.alsongdalsong.roomgame.repository.RoomGameRepository;
import com.curioussong.alsongdalsong.roomgame.service.RoomGameService;
import com.curioussong.alsongdalsong.song.domain.Song;
import com.curioussong.alsongdalsong.song.service.SongService;
import com.curioussong.alsongdalsong.util.KoreanConsonantExtractor;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Pair;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final Integer CONSONANT_HINT_TIME = 15;
    private final Integer SINGER_HINT_TIME = 30;

    private final SimpMessagingTemplate messagingTemplate;
    private final MemberService memberService;
    private final GameRepository gameRepository;
    private final RoomRepository roomRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SongService songService;
    private final RoomGameRepository roomGameRepository;

    private final RoomManager roomManager;

    private ScheduledFuture<?> scheduledTask;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Map<Long, ScheduledExecutorService> roomConsonantHintTimerSchedulers = new ConcurrentHashMap<>(); // 방별 힌트 타이머 스케줄러
    private Map<Long, ScheduledExecutorService> roomSingerHintTimerSchedulers = new ConcurrentHashMap<>(); // 방별 힌트 타이머 스케줄러
    private Map<Long, ScheduledExecutorService> roomRoundTimerSchedulers = new ConcurrentHashMap<>(); // 방별 라운드 타이머 스케줄러
    private final Map<Long, ScheduledExecutorService> roomSchedulers = new ConcurrentHashMap<>(); // 방별 타이머 스케줄러

    @Transactional
    public void startGame(Long channelId, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));

        if (!roomManager.areAllPlayersReady(roomId)) {
            return;
        }
        eventPublisher.publishEvent(new GameStatusEvent(room, "IN_PROGRESS"));
        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);
        sendRoomInfoToSubscriber(destination, room);
        roomManager.initializeGameSetting(roomId);
        startRound(channelId, roomId);

        // TODO : readyStatusMap.remove(roomId);
    }

    public void startRound(Long channelId, Long roomId) {
        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);

        log.info("Round : {} 진행중입니다.", roomManager.getCurrentRound(roomId));

        // 최대 라운드 도달 시 종료
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        int nowRound = roomManager.getCurrentRound(roomId);
        if (nowRound == room.getMaxGameRound()+1) {
            // 전체 게임 종료 시 바로 WAITING으로 변경
            eventPublisher.publishEvent(new GameStatusEvent(room, "WAITING"));

            messagingTemplate.convertAndSend(destination, GameEndResponseDTO.builder()
                            .type("gameEnd")
                            .response(GameEndResponse.builder()
                                    .build())
                    .build());
            return;
        }

        roomConsonantHintTimerSchedulers.put(roomId, Executors.newSingleThreadScheduledExecutor());
        roomSingerHintTimerSchedulers.put(roomId, Executors.newSingleThreadScheduledExecutor());
        roomRoundTimerSchedulers.put(roomId, Executors.newSingleThreadScheduledExecutor());

        log.info("skip 상태 초기화 시작");

        if (room.getMembers() == null) {
            log.info("room.getMembers() is null!");
        } else {
            log.info("Member IDs: {}", room.getMembers());
            System.out.flush(); // 로그 강제 출력
        }

        // skip 상태 초기화
        roomManager.initializeSkipStatus(roomId);

        log.info("{} 번째 라운드 정보 전송", roomManager.getCurrentRound(roomId));
        sendRoundInfo(destination, roomId);

        log.info("카운트 다운 시작");
        startCountdown(destination, roomId);
    }

    public void sendRoundInfo(String destination, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        // 일단 첫번째 모드만 계속 선택
        GameMode gameMode = roomGameRepository.findByRoomId(roomId).getGame().getMode();
//        Random random = new Random();
//        GameMode selectedGameMode = gameModes.get(random.nextInt(gameModes.size()));
//        log.info("선택된 게임 모드: {}", selectedGameMode);

        messagingTemplate.convertAndSend(destination, RoundResponseDTO.builder()
                        .type("roundInfo")
                        .response(RoundResponse.builder()
                                .mode(gameMode)
                                .round(roomManager.getCurrentRound(roomId))
                                .build())
                .build());
    }

    public void startCountdown(String destination, Long roomId) {
        new Thread(() -> {
            try {
                for (int i = 3; i > 0; i--) {
                    sendCountdown(destination, i);
                    Thread.sleep(1000);
                }
                sendCountdown(destination, 0);
                countSongPlayTime(destination, roomManager.getSong(roomId).getPlayTime(), roomId);
                roomManager.initAnswer(roomId);
                sendQuizInfo(destination, roomId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void countSongPlayTime(String destination, int waitTimeInSeconds, Long roomId) {
        roomConsonantHintTimerSchedulers.get(roomId).schedule(() -> sendConsonantHint(destination, roomId), CONSONANT_HINT_TIME, TimeUnit.SECONDS);
        roomSingerHintTimerSchedulers.get(roomId).schedule(() -> sendSingerHint(destination, roomId), SINGER_HINT_TIME, TimeUnit.SECONDS);
        roomRoundTimerSchedulers.get(roomId).schedule(() -> triggerEndEvent(destination), waitTimeInSeconds, TimeUnit.SECONDS);
    }

    public void cancelHintTimer(Long roomId) {
        roomConsonantHintTimerSchedulers.get(roomId).shutdownNow();
        roomSingerHintTimerSchedulers.get(roomId).shutdownNow();

    }

    public void cancelRoundTimerAndTriggerImmediately(String destination, Long roomId) {
        roomRoundTimerSchedulers.get(roomId).shutdownNow();
        triggerEndEvent(destination);
    }

    private void triggerEndEvent(String destination) {
        messagingTemplate.convertAndSend(destination, NextResponseDTO.builder()
                .type("next")
                .build());

        log.info("EndEvent 트리거 활성화");

        Pattern pattern = Pattern.compile("^/topic/channel/(\\d+)/room/(\\d+)$");
        Matcher matcher = pattern.matcher(destination);

        if (matcher.matches()) {
            Long channelId = Long.parseLong(matcher.group(1)); // 첫 번째 캡처 그룹 -> channelId
            Long roomId = Long.parseLong(matcher.group(2)); // 두 번째 캡처 그룹 -> roomId

            log.info("channelId: {}, roomdId : {}", destination, roomId);

            roomManager.nextRound(roomId);
            scheduler.schedule(() -> startRound(channelId, roomId), 5, TimeUnit.SECONDS);
        } else {
            log.info("Pattern did not match destination: {}", destination);
        }
    }

    private void sendConsonantHint(String destination, Long roomId) {
        String consonants = KoreanConsonantExtractor.extractConsonants(roomManager.getSong(roomId).getKorTitle());
        messagingTemplate.convertAndSend(destination, HintResponseDTO.builder()
                        .type("hint")
                        .response(HintResponse.builder()
                                .title(consonants)
                                .build())
                .build());
    }

    private void sendSingerHint(String destination, Long roomId) {
        String consonants = KoreanConsonantExtractor.extractConsonants(roomManager.getSong(roomId).getKorTitle());
        messagingTemplate.convertAndSend(destination, HintResponseDTO.builder()
                .type("hint")
                .response(HintResponse.builder()
                        .title(consonants)
                        .singer(roomManager.getSong(roomId).getArtist())
                        .build())
                .build());
    }

    private void sendQuizInfo(String destination, Long roomId) {
        log.info("퀴즈 정보 전송");
        messagingTemplate.convertAndSend(destination, QuizResponseDTO.builder()
                        .type("quizInfo")
                        .response(QuizResponse.builder()
                                .songUrl(roomManager.getSong(roomId).getUrl())
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
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        sendRoomInfoToSubscriber(destination, room);
        sendUserInfoToSubscriber(destination, room);
    }

    private void sendRoomInfoToSubscriber(String destination, Room room) {
        // 임시 하드코딩
        List<GameMode> gameModes = new ArrayList<>();
        gameModes.add(GameMode.FULL);

        messagingTemplate.convertAndSend(destination, RoomInfoResponseDTO.builder()
                .type("roomInfo")
                .response(RoomInfoResponse.builder()
                        .roomTitle(room.getTitle())
                        .maxPlayer(room.getMaxPlayer())
                        .maxGameRound(room.getMaxGameRound())
                        .hasPassword(room.getPassword()!=null&&!room.getPassword().isEmpty())
                        .format(room.getFormat())
                        .status(room.getStatus())
                        .mode(gameModes)
                        .selectedYear(roomManager.getSelectedYears(room.getId()))
                        .build())
                .build());
    }

    private void sendUserInfoToSubscriber(String destination, Room room) {
        List<Long> memberIds = room.getMembers().stream()
                .map(Member::getId).toList();
        List<UserInfo> userInfoList = new ArrayList<>();

        boolean allReady = true;

        for (Long memberId : memberIds) {
            Member member = memberService.getMemberById(memberId);
            boolean isHost = memberId.equals(room.getHost().getId());
            boolean isReady = Boolean.TRUE.equals(roomManager.getReady(room.getId(), memberId));

            log.debug("User {} ready status in response: {}", member.getUsername(), isReady);

            if (!isReady) {
                allReady = false;
            }

            UserInfo userInfo = new UserInfo(member.getUsername(), isReady, isHost);
            userInfoList.add(userInfo);
        }

        messagingTemplate.convertAndSend(destination, UserInfoResponseDTO
                .builder()
                .type("userInfo")
                .response(UserInfoResponse.builder()
                        .userInfoList(userInfoList)
                        .allReady(allReady)
                        .build())
                .build());
    }

    public boolean checkAnswer(ChatRequest chatRequest, Long roomId) {
        // 채팅의 모든 공백 제거 및 대문자 치환
        String message = chatRequest.getRequest().getMessage().replaceAll("\\s+", "").toUpperCase();

        int nowRound = roomManager.getCurrentRound(roomId);
        // 한글/영어 이외의 문자가 나오면 자름. (ex. 러브일일구(러브119) -> 러브일일구)
        // 공백 제거
        String koreanAnswer = roomManager.getSong(roomId).getKorTitle().replaceAll("[^가-힣].*", "").replaceAll("\\s+", "");
        // 영어는 대문자로 치환
        String englishAnswer = roomManager.getSong(roomId).getEngTitle();

        // 영어 제목이 있는 노래인 경우 한글 제목과 영어 제목 둘 다 정답 처리
        if (englishAnswer != null) {
            englishAnswer = englishAnswer.replaceAll("[^a-zA-Z].*", "").replaceAll("\\s+", "").toUpperCase();
            return message.equals(koreanAnswer) || message.equals(englishAnswer);
        }
        return message.equals(koreanAnswer);
    }

    public void handleAnswer(String userName, Long channelId, Long roomId) {
        // 이미 정답을 맞춘 라운드이면 통과
        if (roomManager.isAnswered(roomId)) {
            return;
        }

        roomManager.setAnswer(roomId);
        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);
        cancelHintTimer(roomId);

        Song song = roomManager.getSong(roomId);
        String koreanTitle = song.getKorTitle();
        String englishTitle = song.getEngTitle();
        StringBuilder title = new StringBuilder();
        if (englishTitle != null) {
            title.append(koreanTitle).append("(").append(englishTitle).append(")");
        } else {
            title.append(koreanTitle);
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

    @Transactional
    public void incrementSkipCount(Long roomId, Long channelId, String username) {
        Long memberId = memberService.getMemberByToken(username).getId();

        // 이미 스킵을 한 사용자라면
        if (roomManager.isSkipped(roomId, memberId)) {
            return;
        }

        // 스킵 상태 변환
        roomManager.setSkip(roomId, memberId);
        int currentSkipCount = roomManager.raiseSkip(roomId);
        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        int participantCount = room.getMembers().size();


        log.debug("Room {} skip count: {}/{}", roomId, currentSkipCount, participantCount);

        // 스킵 정보 전송
        messagingTemplate.convertAndSend(destination, SkipResponseDTO.builder()
                .type("skip")
                .response(SkipResponse.builder()
                        .skipPerson(currentSkipCount)
                        .build())
                .build());


        // 참가자 절반 초과 시 즉시 다음 라운드 시작
        if (currentSkipCount > participantCount / 2) {
            log.info("Skip count exceeded threshold, moving to next round.");
            cancelHintTimer(roomId);
            cancelRoundTimerAndTriggerImmediately(destination, roomId);
        }
    }

//    Todo: 방에 입장한 유저만 레디 상태 변경이 가능하도록 변경 필요
    @Transactional
    public void toggleReady(String username, Long channelId, Long roomId) {
        Map<Long, Boolean> roomReadyStatus = roomManager.getReadyStatus(roomId);

        boolean currentReady = Boolean.TRUE.equals(roomReadyStatus.getOrDefault(username, false));

        boolean newReady = !currentReady;
        log.debug("User {} ready status: {} -> {}", username, currentReady, newReady);

        Long memberId = memberService.getMemberByToken(username).getId();
        roomReadyStatus.put(memberId, !currentReady);

        log.debug("Room {} ready status map: {}", roomId, roomReadyStatus);

        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("방을 찾을 수 없습니다."));
        sendUserInfoToSubscriber(destination, room);
    }

//    Todo: 방에 입장 시 사용자 레디 상태를 false로 세팅, joinRoom 기능 개발 이후 점검 필요
    @EventListener
    public void handleUserJoinedEvent(UserJoinedEvent event) {
        Member member = memberService.getMemberByToken(event.username());
        // host는 방을 만들면서 status가 초기화 되어 있음.
        // host 외에 다른 사람이 방에 입장할 때 status 설정 해줘야 함.
        if (roomManager.getRoomInfo(event.roomId()).getMemberReadyStatus().get(member.getId()) == null) {
            roomManager.getReadyStatus(event.roomId()).put(member.getId(), false);
            roomManager.getSkipStatus(event.roomId()).put(member.getId(), false);
        }
    }

    public void updateSongYears(Long roomId, List<Long> selectedYears) {
        roomManager.setSelectedYears(roomId, selectedYears);
//        roomAndSelectedYears.put(roomId, selectedYears);
    }

    public Game getGameByMode(com.curioussong.alsongdalsong.game.domain.GameMode mode) {
        return gameRepository.findByMode(mode)
                .orElseThrow(() -> new RuntimeException("Game with mode " + mode + " not found"));
    }

    @EventListener
    public void handleUserLeavedEvent(UserLeavedEvent event) {
        Member member = memberService.getMemberByToken(event.username());
        Room room = roomRepository.findById(event.roomId())
                .orElseThrow(() -> new EntityNotFoundException("해당하는 방이 없습니다."));
        room.removeMember(member);
        roomManager.deleteMember(event.roomId(), member.getId());
    }
}