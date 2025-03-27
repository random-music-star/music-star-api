package com.curioussong.alsongdalsong.room.controller;

import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.service.MemberService;
import com.curioussong.alsongdalsong.room.dto.*;
import com.curioussong.alsongdalsong.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final MemberService memberService;

    @PostMapping()
    public ResponseEntity<CreateResponse> createRoom(@RequestHeader("Authorization") String token,
                                                     @RequestBody CreateRequest request) {
        Member member = memberService.getMemberByToken(token);
        CreateResponse response = roomService.createRoom(member, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping()
    public ResponseEntity<?> updateRoom(@RequestHeader("Authorization") String token,
                                        @RequestBody UpdateRequest request){

        Member member = memberService.getMemberByToken(token);
        roomService.updateRoom(member, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<RoomDTO>> getRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        Page<RoomDTO> roomsPage = roomService.getRooms(page, size);
        return ResponseEntity.ok(roomsPage);
    }

    @PostMapping("/enter")
    public ResponseEntity<EnterRoomResponse> enterRoom(
            @RequestBody EnterRoomRequest request
    ){
        EnterRoomResponse response = roomService.enterRoom(request);
        return ResponseEntity.ok(response);
    }
}
