package com.curioussong.alsongdalsong.room.controller;

import com.curioussong.alsongdalsong.room.dto.CreateRequest;
import com.curioussong.alsongdalsong.room.dto.CreateResponse;
import com.curioussong.alsongdalsong.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping()
    public ResponseEntity<CreateResponse> createRoom(@RequestBody CreateRequest request) {
        CreateResponse response = roomService.createRoom(request);
        return ResponseEntity.ok(response);
    }
}
