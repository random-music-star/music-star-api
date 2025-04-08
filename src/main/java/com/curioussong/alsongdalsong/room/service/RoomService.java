package com.curioussong.alsongdalsong.room.service;

import com.curioussong.alsongdalsong.channel.domain.Channel;
import com.curioussong.alsongdalsong.channel.repository.ChannelRepository;
import com.curioussong.alsongdalsong.game.domain.Game;
import com.curioussong.alsongdalsong.game.domain.GameMode;
import com.curioussong.alsongdalsong.game.domain.InGameManager;
import com.curioussong.alsongdalsong.game.domain.RoomManager;
import com.curioussong.alsongdalsong.game.dto.userinfo.UserInfo;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import com.curioussong.alsongdalsong.game.repository.GameRepository;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.event.MemberLocationEvent;
import com.curioussong.alsongdalsong.member.repository.MemberRepository;
import com.curioussong.alsongdalsong.room.domain.Room;
import com.curioussong.alsongdalsong.room.dto.*;
import com.curioussong.alsongdalsong.room.event.RoomUpdatedEvent;
import com.curioussong.alsongdalsong.room.event.UserJoinedEvent;
import com.curioussong.alsongdalsong.room.repository.RoomRepository;
import com.curioussong.alsongdalsong.roomgame.domain.RoomGame;
import com.curioussong.alsongdalsong.roomgame.repository.RoomGameRepository;
import com.curioussong.alsongdalsong.roomyear.domain.RoomYear;
import com.curioussong.alsongdalsong.roomyear.repository.RoomYearRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MemberRepository memberRepository;
    private final GameRepository gameRepository;
    private final RoomYearRepository roomYearRepository;
    private final RoomGameRepository roomGameRepository;
    private final ChannelRepository channelRepository;
    private final RoomManager roomManager;
    private final InGameManager inGameManager;
    private final GameMessageSender gameMessageSender;

    private static final List<Integer> VALID_YEARS = List.of(1970, 1980, 1990, 2000, 2010, 2020, 2021, 2022, 2023, 2024);

    @Transactional
    public CreateResponse createRoom(Member member, CreateRequest request) {
        Channel channel = channelRepository.findById(request.getChannelId())
                .orElseThrow(() -> new HttpClientErrorException(
                        HttpStatus.BAD_REQUEST,
                        "존재하지 않는 채널입니다."
                ));

        validateRoomSettings(request.getFormat(), request.getMaxPlayer(), request.getMaxGameRound(), request.getPassword(), request.getTitle());

        Room room = createRoomEntity(member, channel, request);
        eventPublisher.publishEvent(new MemberLocationEvent(channel.getId(), member));
        setupRoomGames(room, request.getGameModes());
        setupRoomYears(room, request.getSelectedYears());
        notifyRoomCreation(room, request.getChannelId());
        roomManager.authorizeUser(room.getId(), member.getUsername());
        roomManager.addUserColorNumber(room.getId(), member.getUsername());

        return CreateResponse.builder()
                .channelId(room.getChannel().getId())
                .roomNumber(room.getRoomNumber())
                .roomId(room.getId())
                .build();
    }

    @Transactional
    public void joinRoom(Long channelId, String roomId, String sessionId, String userName) {
        Room room = findRoomById(roomId);
        Member member = findMemberByUsername(userName);

        room.addMember(member);
        roomManager.getReadyStatus(roomId).put(member.getId(), false);
        roomManager.removeAuthorizedUser(userName, room);

        sendRoomAndUserInfo(channelId, roomId, room);

        eventPublisher.publishEvent(new UserJoinedEvent(room.getId(), sessionId, userName));
        eventPublisher.publishEvent(new RoomUpdatedEvent(room, channelId, RoomUpdatedEvent.ActionType.UPDATED));
        eventPublisher.publishEvent(new MemberLocationEvent(channelId, member));
    }

    @Transactional
    public void leaveRoom(Long channelId, String roomId, String userName) {
        log.debug("방 나가기 시작 - roomId: {}, userName: {}", roomId, userName);
        Room room = findRoomById(roomId);
        Member member = findMemberByUsername(userName);

        log.info("나가는 멤버: {}", member.getUsername());

        room.removeMember(member);
        roomRepository.save(room);

        log.info("방 나가기 후 멤버 수: {}", room.getMembers().size());
        boolean isFinished = false;
        if (isHostLeaving(room, member)) {
            isFinished = delegateHost(room, member);
        }

        roomManager.getReadyStatus(roomId).remove(member.getId());
        roomManager.removeUserColorNumber(roomId, userName);
        if (room.getStatus() == Room.RoomStatus.IN_PROGRESS) {
            inGameManager.removeSkipStatusWhoLeaved(roomId, member.getId());
        }

        sendRoomAndUserInfo(channelId, roomId, room);

        if (!isFinished) {
            sendRoomAndUserInfo(channelId, roomId, room);
            eventPublisher.publishEvent(new RoomUpdatedEvent(room, channelId, RoomUpdatedEvent.ActionType.UPDATED));
        }
        eventPublisher.publishEvent(new MemberLocationEvent(channelId, member));
        log.debug("이벤트 발행 완료");
    }

    private boolean isHostLeaving(Room room, Member member) {
        return member.getId().equals(room.getHost().getId());
    }

    private boolean delegateHost(Room room, Member member) {
        List<Member> remainingMembers = room.getMembers();
        if (remainingMembers.isEmpty()) {
            room.updateStatus(Room.RoomStatus.FINISHED);
            roomRepository.save(room);
            eventPublisher.publishEvent(new RoomUpdatedEvent(room, room.getChannel().getId(), RoomUpdatedEvent.ActionType.FINISHED));
            return true;
        } else {
            Member newHost = remainingMembers.get(0);
            room.updateHost(newHost);
            roomRepository.save(room);
            return false;
        }
    }

    @Transactional
    public void updateRoom(Member member, UpdateRequest request) {
        Room room = findRoomById(request.getRoomId());

        validateRoomHost(room, member);

        if(!room.getFormat().name().equals(request.getFormat())) {
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "방 생성 후에는 맵(보드판/점수판)을 변경할 수 없습니다."
            );
        }

        validateRoomSettings(request.getFormat(), request.getMaxPlayer(), request.getMaxGameRound(), request.getPassword(), request.getTitle());

        if (room.getMembers().size() > request.getMaxPlayer()) {
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "방의 인원 제한은 현재 인원이상이어야 합니다."
            );
        }

        updateRoomGames(room, request.getGameModes());
        roomManager.updateGameModes(room, request.getGameModes());

        room.update(request.getTitle(), request.getPassword(),
                request.getMaxPlayer(), request.getMaxGameRound());

        updateRoomYears(room, request.getSelectedYears());

        roomManager.updateRoomInfo(room, request.getSelectedYears());
        eventPublisher.publishEvent(new RoomUpdatedEvent(room, room.getChannel().getId(), RoomUpdatedEvent.ActionType.UPDATED));
    }

    @Transactional(readOnly=true)
    public Page<RoomDTO> getRooms(Long channelId, int page, int size) {
        if (channelId == null) {
            throw new IllegalArgumentException("channelId는 필수입니다.");
        }

        Pageable pageable = PageRequest.of(page, size);

        List<Room.RoomStatus> activeStatuses = Arrays.asList(
                Room.RoomStatus.WAITING,
                Room.RoomStatus.IN_PROGRESS
        );

        // 1. 기존 방식으로 방 목록 조회
        Page<Room> roomPage = roomRepository.findByChannelIdAndStatusInOrderByUpdatedAtDesc(
                channelId, activeStatuses, pageable
        );

        List<Room> rooms = roomPage.getContent();
        if (rooms.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // 2. 방 ID 목록 추출
        List<String> roomIds = rooms.stream()
                .map(Room::getId)
                .collect(Collectors.toList());

        // 3. 한 번에 모든 방의 RoomYear와 RoomGame 정보 조회
        List<RoomYear> allRoomYears = roomYearRepository.findByRoomIdIn(roomIds);
        List<RoomGame> allRoomGames = roomGameRepository.findByRoomIdIn(roomIds);

        // 4. 방 ID별로 그룹화
        Map<String, List<Integer>> yearsByRoomId = new HashMap<>();
        for (RoomYear roomYear : allRoomYears) {
            String roomId = roomYear.getRoom().getId();
            yearsByRoomId.computeIfAbsent(roomId, k -> new ArrayList<>())
                    .add(roomYear.getYear());
        }

        Map<String, List<GameMode>> gameModesByRoomId = new HashMap<>();
        for (RoomGame roomGame : allRoomGames) {
            String roomId = roomGame.getRoom().getId();
            gameModesByRoomId.computeIfAbsent(roomId, k -> new ArrayList<>())
                    .add(roomGame.getGame().getMode());
        }

        // 5. DTO 변환
        List<RoomDTO> roomDtos = rooms.stream()
                .map(room -> {
                    RoomDTO baseDto = room.toDto();
                    String roomId = room.getId();

                    // GameMode와 years 정보 설정
                    baseDto.setGameModes(gameModesByRoomId.get(roomId));
                    baseDto.setYears(yearsByRoomId.get(roomId));

                    return baseDto;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(roomDtos, pageable, roomPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public boolean canEnterRoom(String roomId) {
        return !isRoomFinished(roomId) && !isRoomFull(roomId) && !isRoomInProgress(roomId);
    }

    public boolean isRoomFull(String roomId) {
        Room room = findRoomById(roomId);
        return room.getMembers().size() == room.getMaxPlayer();
    }

    public boolean isRoomInProgress(String roomId) {
        Room room = findRoomById(roomId);
        return room.getStatus() == Room.RoomStatus.IN_PROGRESS;
    }

    public boolean isRoomFinished(String roomId) {
        Room room = findRoomById(roomId);
        return room.getStatus() == Room.RoomStatus.FINISHED;
    }

    @Transactional
    public EnterRoomResponse enterRoom(String userName, EnterRoomRequest request) {
        Room room = findRoomById(request.getRoomId());

        if(!room.getPassword().equals(request.getPassword())) {
            return EnterRoomResponse.builder()
                    .roomId(request.getRoomId())
                    .success(false)
                    .build();
        }

        roomManager.authorizeUser(room.getId(), userName);
        roomManager.addUserColorNumber(room.getId(), userName);

        return EnterRoomResponse.builder()
                .roomId(request.getRoomId())
                .success(true)
                .build();
    }

    public boolean isAuthorizedUser(String roomId, String userName) {
        return roomManager.getAuthorizedUser(roomId).containsKey(userName);
    }

    public boolean hasPassword(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new HttpClientErrorException(
                        HttpStatus.BAD_REQUEST,
                        "존재하지 않는 방 번호입니다."
                ));
        log.info("Password : {}", room.getPassword());

        return !room.getPassword().isBlank();
    }

    private Long generateRoomNumber(Long channelId) {
        Long maxRoomNumber = roomRepository.findMaxRoomNumberByChannelId(channelId);
        return (maxRoomNumber != null) ? maxRoomNumber + 1 : 1;
    }


    private void validateRoomSettings(String format, Integer maxPlayer, Integer maxGameRound, String password, String title){
        if("BOARD".equals(format)){
            if(maxPlayer>6 || maxPlayer<2) {
                throw new HttpClientErrorException(
                        HttpStatus.BAD_REQUEST,
                        "보드판 맵의 최대 인원은 2~6명입니다."
                );
            }
        } else if ("GENERAL".equals(format)) {
            if(maxPlayer>60 || maxPlayer<2){
                throw new HttpClientErrorException(
                        HttpStatus.BAD_REQUEST,
                        "점수판 맵의 최대 인원은 2~60명입니다. "
                );
            }
        }

        if(!(maxGameRound == 10 || maxGameRound == 20 || maxGameRound == 30)){
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "게임 라운드는 10, 20, 30 중 하나여야 합니다."
            );
        }

        if(password.length()>30){
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "방의 비밀번호는 30자 이하여야 합니다."
            );
        }

        if(title.length() > 15){
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "방의 제목은 15 이내여야 합니다."
            );
        }
    }

    private List<Game> getGamesFromModes(List<GameMode> gameModes) {
        if (gameModes == null || gameModes.isEmpty()) {
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "게임 모드를 하나 이상 선택해야 합니다."
            );
        }
        return gameModes.stream()
                .map(gameMode -> gameRepository.findByMode(gameMode)
                        .orElseThrow(() -> new HttpClientErrorException(
                                HttpStatus.BAD_REQUEST,
                                "지원하지 않는 게임모드가 포함되어 있습니다."
                        )))
                .toList();
    }

    private List<RoomYear> createRoomYears(Room room, List<Integer> selectedYears) {
        return selectedYears.stream()
                .distinct()
                .map(year -> new RoomYear(room, year))
                .toList();
    }

    private void validateYears(List<Integer> years) {
        if (years == null || years.isEmpty()) {
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "선택된 연도가 없습니다."
            );
        }

        for (Integer year : years) {
            if (year == null || !VALID_YEARS.contains(year)) {
                throw new HttpClientErrorException(
                        HttpStatus.BAD_REQUEST,
                        "지원하지 않는 연도가 포함되어 있습니다"
                );
            }
        }
    }

    private Room findRoomById(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("해당하는 방이 없습니다."));
    }

    private Member findMemberByUsername(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("해당 회원이 없습니다."));
    }

    private void sendRoomAndUserInfo(Long channelId, String roomId, Room room) {
        String destination = String.format("/topic/channel/%d/room/%s", channelId, roomId);

        gameMessageSender.sendRoomInfo(destination, room,
                roomManager.getSelectedYears(roomId),
                roomManager.getGameModes(roomId));

        List<UserInfo> userInfoList = roomManager.getUserInfos(room);
        boolean allReady = roomManager.isAllReady(room);
        gameMessageSender.sendUserInfo(destination, userInfoList, allReady);
    }

    private void validateRoomHost(Room room, Member member) {
        if (!room.getHost().getId().equals(member.getId())) {
            throw new HttpClientErrorException(
                    HttpStatus.UNAUTHORIZED,
                    "방 설정은 방장만 변경 가능합니다."
            );
        }
    }

    private void updateRoomGames(Room room, List<GameMode> gameModes) {
        roomGameRepository.deleteAllByRoom(room);
        List<Game> games = getGamesFromModes(gameModes);
        List<RoomGame> roomGames = games.stream()
                .map(game -> new RoomGame(game, room))
                .toList();
        roomGameRepository.saveAll(roomGames);
    }

    private void updateRoomYears(Room room, List<Integer> selectedYears) {
        roomYearRepository.deleteAllByRoom(room);
        roomYearRepository.flush();

        validateYears(selectedYears);
        List<RoomYear> newRoomYears = createRoomYears(room, selectedYears);
        roomYearRepository.saveAll(newRoomYears);
    }

    private Room createRoomEntity(Member member, Channel channel, CreateRequest request) {
        Room room = Room.builder()
                .channel(channel)
                .host(member)
                .title(request.getTitle())
                .password(request.getPassword())
                .maxPlayer(request.getMaxPlayer())
                .maxGameRound(request.getMaxGameRound())
                .format(Room.RoomFormat.valueOf(request.getFormat()))
                .build();

        Long roomNumber = generateRoomNumber(room.getChannel().getId());
        room.assignRoomNumber(roomNumber);

        room.addMember(member);
        return roomRepository.save(room);
    }

    private void setupRoomGames(Room room, List<GameMode> gameModes) {
        List<Game> games = getGamesFromModes(gameModes);
        List<RoomGame> roomGames = games.stream()
                .map(game -> new RoomGame(game, room))
                .toList();
        roomGameRepository.saveAll(roomGames);
    }

    private void setupRoomYears(Room room, List<Integer> selectedYears) {
        validateYears(selectedYears);
        List<RoomYear> roomYears = createRoomYears(room, selectedYears);
        roomYearRepository.saveAll(roomYears);
    }
    private void notifyRoomCreation(Room room, Long channelId) {
        eventPublisher.publishEvent(new RoomUpdatedEvent(room, room.getChannel().getId(), RoomUpdatedEvent.ActionType.CREATED));
        roomManager.addRoomInfo(room, channelId);
    }

    @Transactional(readOnly = true)
    public SearchResultResponse searchRoomByTitle(Long channelId, String title) {
        if(channelId < 1 || channelId > 7) {
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "존재하지 않는 채널입니다."
            );
        }

        List<Room> rooms = roomRepository.findByChannelIdAndTitleContainingAndStatusOrderByUpdatedAtDesc(channelId, title, Room.RoomStatus.WAITING);

        List<SearchResultDTO> roomDtos = rooms.stream()
                .map(room -> new SearchResultDTO(
                        room.getFormat(),
                        room.getId(),
                        room.getRoomNumber(),
                        room.getTitle(),
                        room.getMembers().size(),
                        room.getMaxPlayer(),
                        !room.getPassword().isEmpty()
                ))
                .toList();
        log.debug("roomDtos: {}", roomDtos);
        return new SearchResultResponse(roomDtos);
    }

    @Transactional(readOnly = true)
    public SearchResultResponse searchRoomByNumber(Long channelId, Long number) {
        if(channelId < 1 || channelId > 7) {
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "존재하지 않는 채널입니다."
            );
        }

        Optional<Room> roomDto = roomRepository.findByChannelIdAndRoomNumberAndStatusOrderByUpdatedAtDesc(channelId, number, Room.RoomStatus.WAITING);

        List<SearchResultDTO> response = roomDto.stream()
                .map(room -> new SearchResultDTO(
                        room.getFormat(),
                        room.getId(),
                        room.getRoomNumber(),
                        room.getTitle(),
                        room.getMembers().size(),
                        room.getMaxPlayer(),
                        !room.getPassword().isEmpty()
                ))
                .toList();

        return new SearchResultResponse(response);
    }
}