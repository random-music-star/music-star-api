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
