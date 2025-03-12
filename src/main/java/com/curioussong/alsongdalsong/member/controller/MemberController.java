package com.curioussong.alsongdalsong.member.controller;

import com.curioussong.alsongdalsong.member.dto.loginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
@Slf4j
public class MemberController {

    @GetMapping("/login")
    ResponseEntity<loginResponse> getToken() {
        String token = UUID.randomUUID().toString();
        log.info("사용자 임의 토큰 값 : {}", token);
        return ResponseEntity.ok(new loginResponse(token));
    }
}
