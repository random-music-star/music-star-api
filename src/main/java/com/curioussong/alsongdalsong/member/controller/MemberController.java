package com.curioussong.alsongdalsong.member.controller;

import com.curioussong.alsongdalsong.member.dto.*;
import com.curioussong.alsongdalsong.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/guest/login")
    ResponseEntity<GuestLogin> guestLogin() {
        return ResponseEntity.ok(new GuestLogin(memberService.guestLogin()));
    }

    @PostMapping("user/signup")
    ResponseEntity<String> userSignup(@RequestBody UserLoginRequest userLoginRequest) {
        try {
            memberService.userSignup(userLoginRequest.username(), userLoginRequest.password());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 존재하는 회원입니다.");
        }
    }

    @PostMapping("user/login")
    ResponseEntity<UserLoginResponse> userLogin(@RequestBody UserLoginRequest userLoginRequest) {
        try {
            return ResponseEntity.ok(memberService.userLogin(userLoginRequest.username(), userLoginRequest.password()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    @GetMapping("{channelId}")
    public ResponseEntity<List<MemberStatusDTO>> getChannelMembers(@PathVariable("channelId") Long channelId) {
        List<MemberStatusDTO> members = memberService.getChannelMembers(channelId);
        return ResponseEntity.ok(members);
    }

    @PatchMapping("/color")
    public ResponseEntity<Void> changeCharacterColor(@RequestBody ColorChangeRequest colorChangeRequest) {
        memberService.changeCharacterColor(colorChangeRequest.username(), colorChangeRequest.colorCode());
        return ResponseEntity.ok().build();
    }
}
