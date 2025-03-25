package com.curioussong.alsongdalsong.member.controller;

import com.curioussong.alsongdalsong.member.dto.GuestLogin;
import com.curioussong.alsongdalsong.member.dto.UserLoginRequest;
import com.curioussong.alsongdalsong.member.dto.UserLoginResponse;
import com.curioussong.alsongdalsong.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
