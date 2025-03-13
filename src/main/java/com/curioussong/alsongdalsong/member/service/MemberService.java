package com.curioussong.alsongdalsong.member.service;

import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public String guestLogin() {
        String token = "guest_" + UUID.randomUUID().toString().substring(0, 8);
        log.info("사용자 임의 토큰 값 : {}", token);

        // 멤버 생성
        Member member = Member.builder()
                        .type(Member.MemberType.GUEST)
                        .username(token)
                        .build();

        memberRepository.save(member);
        return token;
    }

    public  Member getMemberByToken(String token) {
        return memberRepository.findByUsername(token);
    }
}
