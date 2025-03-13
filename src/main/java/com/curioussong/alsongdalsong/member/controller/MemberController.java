package com.curioussong.alsongdalsong.member.controller;

import com.curioussong.alsongdalsong.member.dto.guestLogin;
import com.curioussong.alsongdalsong.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/guest/login")
    ResponseEntity<guestLogin> guestLogin() {
        return ResponseEntity.ok(new guestLogin(memberService.guestLogin()));
    }
}
