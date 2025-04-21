package com.curioussong.alsongdalsong.auth.service;

import com.curioussong.alsongdalsong.common.util.BadWordFilter;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.dto.UserLoginResponse;
import com.curioussong.alsongdalsong.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final MemberRepository memberRepository;
//    private final ToxicUserName toxicUserName;

    public String guestLogin() {
        Member lastGuestMember = memberRepository.findFirstByTypeOrderByIdDesc(Member.MemberType.GUEST);

        String token = createGuestToken(lastGuestMember);
        log.debug("게스트 토큰 생성됨: {}", token);

        Member member = Member.builder()
                .type(Member.MemberType.GUEST)
                .username(token)
                .colorCode("#f8fc03")
                .build();

        memberRepository.save(member);
        return token;
    }

    private String createGuestToken(Member lastGuestMember) {
        if (lastGuestMember == null) {
            return "guest_0";
        }
        return "guest_" + (Integer.parseInt(lastGuestMember.getUsername().replace("guest_", "")) + 1);
    }

    public void userSignup(String username, String password) {
        validateSignupInput(username, password);

        Member member = Member.builder()
                .username(username)
                .password(password)
                .type(Member.MemberType.USER)
                .colorCode("#f8fc03")
                .build();

        memberRepository.save(member);
    }

    private void validateSignupInput(String username, String password) {
        if (memberRepository.findByUsername(username).isPresent()) {
            throw new HttpClientErrorException(
                    HttpStatus.CONFLICT,
                    "이미 존재하는 회원입니다.");
        }

        if (username == null || username.isEmpty()) {
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "사용자 이름은 필수 항목입니다."
            );
        }

        if (username.contains(" ")) {
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "사용자 이름에는 공백을 포함할 수 없습니다."
            );
        }

        if (!username.matches("^[a-zA-Z0-9]+$")) {
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "사용자 이름은 영어와 숫자만 포함할 수 있습니다."
            );
        }

        if (password == null || password.isEmpty()) {
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "비밀번호는 필수 항목입니다."
            );
        }

        if (BadWordFilter.containsBadWord(username)) {
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "부적절한 닉네임입니다."
            );
        }
    }

    public UserLoginResponse userLogin(String username, String password) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new HttpClientErrorException(
                        HttpStatus.NOT_FOUND,
                        "존재하지 않는 회원입니다."
                ));

        if (!password.equals(member.getPassword())) {
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "비밀번호가 틀렸습니다."
            );
        }

        return new UserLoginResponse(username);
    }
}