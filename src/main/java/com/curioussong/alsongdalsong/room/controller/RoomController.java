package com.curioussong.alsongdalsong.room.controller;

import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.service.MemberService;
import com.curioussong.alsongdalsong.room.dto.*;
import com.curioussong.alsongdalsong.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final MemberService memberService;

    @PostMapping()
    public ResponseEntity<CreateResponse> createRoom(@RequestBody CreateRequest request) {
        Member member = memberService.getCurrentMember();
        CreateResponse response = roomService.createRoom(member, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping()
    public ResponseEntity<?> updateRoom(@RequestBody UpdateRequest request){
        Member member = memberService.getCurrentMember();
        roomService.updateRoom(member, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping()
    public ResponseEntity<Page<RoomDTO>> getRooms(
            @RequestParam Long channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        Page<RoomDTO> roomsPage = roomService.getRooms(channelId, page, size);
        return ResponseEntity.ok(roomsPage);
    }

    @PostMapping("/enter")
    public ResponseEntity<EnterRoomResponse> enterRoom(@RequestBody EnterRoomRequest request){
        Member member = memberService.getCurrentMember();
        EnterRoomResponse response = roomService.enterRoom(member.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/title")
    public ResponseEntity<SearchResultResponse> searchRoomByTitle(
            @RequestParam Long channelId,
            @RequestParam String title){
        SearchResultResponse response = roomService.searchRoomByTitle(channelId, title);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/number")
    public ResponseEntity<SearchResultResponse> searchRoomByNumber(
            @RequestParam Long channelId,
            @RequestParam Long roomNumber){
        SearchResultResponse response = roomService.searchRoomByNumber(channelId, roomNumber);
        return ResponseEntity.ok(response);
    }
}
