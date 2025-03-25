package com.curioussong.alsongdalsong.member.service;

import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.dto.UserLoginResponse;
import com.curioussong.alsongdalsong.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
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
        return memberRepository.findByUsername(token).orElseThrow(() -> new EntityNotFoundException("해당 회원이 없습니다."));
    }

    public Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new EntityNotFoundException("해당 회원이 없습니다."));
    }

    public void userSignup(String username, String password) {
        if (memberRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 회원입니다.");
        }

        Member member = Member.builder()
                .username(username)
                .password(password)
                .type(Member.MemberType.USER)
                .build();

        memberRepository.save(member);
    }

    public UserLoginResponse userLogin(String username, String password) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!password.equals(member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
        }

        return new UserLoginResponse(username);
    }
}
