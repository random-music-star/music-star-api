package com.curioussong.alsongdalsong.member.controller;

import com.curioussong.alsongdalsong.member.dto.guestLogin;
import com.curioussong.alsongdalsong.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/guest/signup")
    ResponseEntity<guestLogin> getToken() {
        return ResponseEntity.ok(new guestLogin(memberService.getToken()));
    }

    @PostMapping("/guest/login")
    ResponseEntity<Void> guestLogin(@RequestBody guestLogin guestInfo) {
        memberService.guestLogin(guestInfo.token());

        return ResponseEntity.ok().build();
    }
}
