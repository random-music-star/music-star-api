package com.curioussong.alsongdalsong.auth.controller;

import com.curioussong.alsongdalsong.auth.dto.UserSignupRequest;
import com.curioussong.alsongdalsong.auth.service.AuthService;
import com.curioussong.alsongdalsong.jwt.JwtTokenProvider;
import com.curioussong.alsongdalsong.jwt.dto.TokenResponse;
import com.curioussong.alsongdalsong.auth.dto.UserLoginRequest;
import com.curioussong.alsongdalsong.auth.dto.UserLoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody UserSignupRequest request) {
        authService.userSignup(request.username(), request.password());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody UserLoginRequest request) {
        UserLoginResponse user = authService.userLogin(request.username(), request.password());
        String token = jwtTokenProvider.createToken(user.username());
        return ResponseEntity.ok(new TokenResponse(token));
    }

    @GetMapping("/guest")
    public ResponseEntity<TokenResponse> guestLogin() {
        String guestToken = authService.guestLogin();
        String token = jwtTokenProvider.createToken(guestToken);
        return ResponseEntity.ok(new TokenResponse(token));
    }
}
