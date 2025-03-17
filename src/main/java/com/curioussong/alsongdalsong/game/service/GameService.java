package com.curioussong.alsongdalsong.game.service;

import com.curioussong.alsongdalsong.chat.dto.ChatRequest;
import com.curioussong.alsongdalsong.game.domain.Game;
import com.curioussong.alsongdalsong.game.domain.Game.GameMode;
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
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import com.curioussong.alsongdalsong.room.service.RoomService;
import com.curioussong.alsongdalsong.roomgame.domain.RoomGame;
import com.curioussong.alsongdalsong.roomgame.service.RoomGameService;
import com.curioussong.alsongdalsong.song.domain.Song;
import com.curioussong.alsongdalsong.song.service.SongService;
import com.curioussong.alsongdalsong.util.KoreanConsonantExtractor;
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

    private final Integer CONSONANT_HINT_TIME = 10;
    private final Integer SINGER_HINT_TIME = 15;

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;
    private final MemberService memberService;
    private final GameRepository gameRepository;
    private final RoomRepository roomRepository;
    private final RoomGameService roomGameService;
    private final ApplicationEventPublisher eventPublisher;
    private final SongService songService;

    private Map<Long, Integer> roomAndRound = new HashMap<>(); // <roomId, round> 쌍으로 저장
    private Map<Long, Map<Integer,GameMode>> roundAndMode = new HashMap<>(); // <roomId, <round, FULL>> 쌍으로 저장
    private Map<Long, Map<Integer, Song>> roundAndSong = new HashMap<>(); // <roomId, <round, Song>> 쌍으로 저장p
    private Map<Long, Integer> skipCount = new HashMap<>();
    private Map<Long, Boolean> isAnswered = new HashMap<>(); // 방에서 정답을 맞추면 true 상태. 정답을 못맞추는 동안에는 false
    private Map<Pair<Long, Long>, Boolean> isSkipped = new HashMap<>(); // <<roomId, memberId>, false>> 로 저장
    private Map<Long, List<Integer>> roomAndSelectedYears = new HashMap<>();

    private ScheduledFuture<?> scheduledTask;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Map<Long, ScheduledExecutorService> roomConsonantHintTimerSchedulers = new ConcurrentHashMap<>(); // 방별 힌트 타이머 스케줄러
    private Map<Long, ScheduledExecutorService> roomSingerHintTimerSchedulers = new ConcurrentHashMap<>(); // 방별 힌트 타이머 스케줄러
    private Map<Long, ScheduledExecutorService> roomRoundTimerSchedulers = new ConcurrentHashMap<>(); // 방별 라운드 타이머 스케줄러
    private final Map<Long, ScheduledExecutorService> roomSchedulers = new ConcurrentHashMap<>(); // 방별 타이머 스케줄러
    private final Map<Long, Map<String, Boolean>> readyStatusMap = new ConcurrentHashMap<>();

    @Transactional
    public void startGame(Long channelId, Long roomId) {
        if (!areAllPlayersReady(roomId)) {
            return;
        }
        eventPublisher.publishEvent(new GameStatusEvent(roomId, RoomStatus.IN_PROGRESS));
        Room room = roomRepository.findById(roomId).orElse(null);
        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);
        sendRoomInfoToSubscriber(destination, room);
        initializeGameSetting(roomId);
        startRound(channelId, roomId);

        readyStatusMap.remove(roomId);
    }

    private boolean areAllPlayersReady(Long roomId) {
        Room room = roomService.findRoomById(roomId);
        List<Member> members = room.getMembers();
        Map<String, Boolean> roomReadyStatus = readyStatusMap.getOrDefault(roomId, new ConcurrentHashMap<>());

        for (Member member : members) {;
            boolean isReady = Boolean.TRUE.equals(roomReadyStatus.getOrDefault(member.getUsername(), false));

            if (!isReady) {
                return false;
            }
        }

        return true;
    }

    public void startRound(Long channelId, Long roomId) {
        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);

        log.info("Round : {} 진행중입니다.", roomAndRound.get(roomId));

        // 최대 라운드 도달 시 종료
        Room room = roomService.findRoomById(roomId);
        int nowRound = roomAndRound.get(roomId);
        if (nowRound == room.getMaxGameRound()+1) {
            // 전체 게임 종료 시 바로 WAITING으로 변경
            eventPublisher.publishEvent(new GameStatusEvent(roomId, RoomStatus.WAITING));

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
        for (Member member : room.getMembers()) {
            isSkipped.put(Pair.of(roomId, member.getId()), false);
        }

        log.info("{} 번째 라운드 정보 전송", roomAndRound.get(roomId));
        sendRoundInfo(destination, roomId);
        skipCount.put(roomId, 0);

        log.info("카운트 다운 시작");
        startCountdown(destination, roomId);
    }

    private void initializeGameSetting(Long roomId) {
        Room room = roomService.findRoomById(roomId);
        Map<Integer, GameMode> roundMap = new HashMap<>();
        Map<Integer, Song> songMap = new HashMap<>();
        List<Song> selectedSongs = songService.getRandomSongByYear(roomAndSelectedYears.get(roomId), room.getMaxGameRound());
        for (int i=1;i<=room.getMaxGameRound();i++) { // 현재는 한 게임당 20라운드 하드코딩
            roundMap.put(i,GameMode.FULL);
            songMap.put(i, selectedSongs.get(i-1));
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
                        .response(RoundResponse.builder()
                                .mode(gameMode)
                                .round(roomAndRound.get(roomId))
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
                countSongPlayTime(destination, roundAndSong.get(roomId).get(roomAndRound.get(roomId)).getPlayTime(), roomId);
                isAnswered.put(roomId, false);
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

            roomAndRound.put(roomId, roomAndRound.get(roomId) + 1);
            scheduler.schedule(() -> startRound(channelId, roomId), 5, TimeUnit.SECONDS);
        } else {
            log.info("Pattern did not match destination: {}", destination);
        }
    }

    private void sendConsonantHint(String destination, Long roomId) {
        String consonants = KoreanConsonantExtractor.extractConsonants(roundAndSong.get(roomId).get(roomAndRound.get(roomId)).getKorTitle());
        messagingTemplate.convertAndSend(destination, HintResponseDTO.builder()
                        .type("hint")
                        .response(HintResponse.builder()
                                .title(consonants)
                                .build())
                .build());
    }

    private void sendSingerHint(String destination, Long roomId) {
        String consonants = KoreanConsonantExtractor.extractConsonants(roundAndSong.get(roomId).get(roomAndRound.get(roomId)).getKorTitle());
        messagingTemplate.convertAndSend(destination, HintResponseDTO.builder()
                .type("hint")
                .response(HintResponse.builder()
                        .title(consonants)
                        .singer(roundAndSong.get(roomId).get(roomAndRound.get(roomId)).getArtist())
                        .build())
                .build());
    }

    private void sendQuizInfo(String destination, Long roomId) {
        log.info("퀴즈 정보 전송");
        messagingTemplate.convertAndSend(destination, QuizResponseDTO.builder()
                        .type("quizInfo")
                        .response(QuizResponse.builder()
                                .songUrl(roundAndSong.get(roomId).get(roomAndRound.get(roomId)).getYoutubeUrl())
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

        messagingTemplate.convertAndSend(destination, RoomInfoResponseDTO.builder()
                .type("roomInfo")
                .response(RoomInfoResponse.builder()
                        .roomTitle(room.getTitle())
                        .maxPlayer(room.getMaxPlayer())
                        .maxGameRound(room.getMaxGameRound())
                        .format(room.getFormat())
                        .status(room.getStatus())
                        .mode(gameModes)
                        .selectedYear(roomAndSelectedYears.get(room.getId()))
                        .build())
                .build());
    }

    private void sendUserInfoToSubscriber(String destination, Room room) {
        List<Long> memberIds = room.getMembers().stream()
                .map(Member::getId).toList();
        List<UserInfo> userInfoList = new ArrayList<>();

        boolean allReady = true;

        Map<String, Boolean> roomReadyStatus = readyStatusMap.getOrDefault(room.getId(), new ConcurrentHashMap<>());

        for (Long memberId : memberIds) {
            Member member = memberService.getMemberById(memberId);
            boolean isHost = memberId.equals(room.getHost().getId());
            boolean isReady = Boolean.TRUE.equals(roomReadyStatus.getOrDefault(member.getUsername(), false));

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

        int nowRound = roomAndRound.get(roomId);
        // 한글/영어 이외의 문자가 나오면 자름. (ex. 러브일일구(러브119) -> 러브일일구)
        // 공백 제거
        String koreanAnswer = roundAndSong.get(roomId).get(nowRound).getKorTitle().replaceAll("[^가-힣].*", "").replaceAll("\\s+", "");
        // 영어는 대문자로 치환
        String englishAnswer = roundAndSong.get(roomId).get(nowRound).getEngTitle();

        // 영어 제목이 있는 노래인 경우 한글 제목과 영어 제목 둘 다 정답 처리
        if (englishAnswer != null) {
            englishAnswer = englishAnswer.replaceAll("[^a-zA-Z].*", "").replaceAll("\\s+", "").toUpperCase();
            return message.equals(koreanAnswer) || message.equals(englishAnswer);
        }
        return message.equals(koreanAnswer);
    }

    public void handleAnswer(String userName, Long channelId, Long roomId) {
        // 이미 정답을 맞춘 라운드이면 통과
        if (isAnswered.get(roomId)) {
            return;
        }

        isAnswered.put(roomId, true);
        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);
        cancelHintTimer(roomId);

        String koreanTitle = roundAndSong.get(roomId).get(roomAndRound.get(roomId)).getKorTitle();
        String englishTitle = roundAndSong.get(roomId).get(roomAndRound.get(roomId)).getEngTitle();
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
                                .singer(roundAndSong.get(roomId).get(roomAndRound.get(roomId)).getArtist())
                                .score(1)
                                .build())
                .build());
    }

    @Transactional
    public void incrementSkipCount(Long roomId, Long channelId, String username) {
        Long memberId = memberService.getMemberByToken(username).getId();

        // 이미 스킵을 한 사용자라면
        if (isSkipped.get(Pair.of(roomId, memberId))) {
            return;
        }

        // 스킵 상태 변환
        isSkipped.put(Pair.of(roomId, memberId), true);

        skipCount.put(roomId, skipCount.getOrDefault(roomId, 0) + 1);
        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);

        int participantCount = roomRepository.findById(roomId)
                .map(room -> room.getMembers().size())
                .orElse(0);

        log.debug("Room {} skip count: {}/{}", roomId, skipCount.get(roomId), participantCount);

        // 스킵 정보 전송
        messagingTemplate.convertAndSend(destination, SkipResponseDTO.builder()
                .type("skip")
                .response(SkipResponse.builder()
                        .skipPerson(skipCount.get(roomId))
                        .build())
                .build());


        // 참가자 절반 초과 시 즉시 다음 라운드 시작
        if (skipCount.get(roomId) > participantCount / 2) {
            log.info("Skip count exceeded threshold, moving to next round.");
            cancelHintTimer(roomId);
            cancelRoundTimerAndTriggerImmediately(destination, roomId);
        }
    }

//    Todo: 방에 입장한 유저만 레디 상태 변경이 가능하도록 변경 필요
    @Transactional
    public void toggleReady(String username, Long channelId, Long roomId) {
        Map<String, Boolean> roomReadyStatus = readyStatusMap.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());

        boolean currentReady = Boolean.TRUE.equals(roomReadyStatus.getOrDefault(username, false));

        boolean newReady = !currentReady;
        log.debug("User {} ready status: {} -> {}", username, currentReady, newReady);

        roomReadyStatus.put(username, !currentReady);

        log.debug("Room {} ready status map: {}", roomId, roomReadyStatus);

        String destination = String.format("/topic/channel/%d/room/%d", channelId, roomId);
        Room room = roomService.findRoomById(roomId);
        sendUserInfoToSubscriber(destination, room);
    }

//    Todo: 방에 입장 시 사용자 레디 상태를 false로 세팅, joinRoom 기능 개발 이후 점검 필요
    @EventListener
    public void handleUserJoinedEvent(UserJoinedEvent event) {
        Map<String, Boolean> roomReadyStatus = readyStatusMap.computeIfAbsent(event.roomId(), k -> new ConcurrentHashMap<>());
        roomReadyStatus.put(event.username(), false);

        log.debug("User {} joined room {}. Ready status initialized to false.", event.username(), event.roomId());
    }

    public void updateSongYears(Long roomId, List<Integer> selectedYears) {
        roomAndSelectedYears.put(roomId, selectedYears);
    }
}