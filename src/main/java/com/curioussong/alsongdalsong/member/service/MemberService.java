package com.curioussong.alsongdalsong.member.service;

import com.curioussong.alsongdalsong.channel.domain.Channel;
import com.curioussong.alsongdalsong.channel.repository.ChannelRepository;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.dto.MemberStatusDTO;
import com.curioussong.alsongdalsong.member.dto.UserLoginResponse;
import com.curioussong.alsongdalsong.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final ChannelRepository channelRepository;

    public String guestLogin() {
        Member lastGuestMember = memberRepository.findFirstByTypeOrderByIdDesc(Member.MemberType.GUEST);

        String token = createGuestToken(lastGuestMember);
        log.info("사용자 임의 토큰 값 : {}", token);

        // 멤버 생성
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
        return "guest_" + (Integer.parseInt(lastGuestMember.getUsername().replace("guest_", ""))+1);
    }

    public  Member getMemberByToken(String token) {
        return memberRepository.findByUsername(token).orElseThrow(() -> new HttpClientErrorException(
                HttpStatus.NOT_FOUND,
                "해당 회원이 없습니다."
        ));
    }

    public Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new EntityNotFoundException("해당 회원이 없습니다."));
    }

    public void userSignup(String username, String password) {
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

        Member member = Member.builder()
                .username(username)
                .password(password)
                .type(Member.MemberType.USER)
                .colorCode("#f8fc03")
                .build();

        memberRepository.save(member);
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

    @Transactional
    public void enterChannel(String username, Long channelId) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("해당 멤버를 찾을 수 없습니다."));

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("해당 채널을 찾을 수 없습니다."));

        member.enterChannel(channel);
        log.info("사용자 {} 가 채널 {} 에 입장하였습니다.", username, channelId);
        memberRepository.save(member);
    }

    @Transactional
    public void leaveChannel(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("해당 멤버를 찾을 수 없습니다."));

        member.leaveChannel();
        log.info("사용자 {} 가 채널에서 퇴장하였습니다.", username);
        memberRepository.save(member);
    }

    public List<MemberStatusDTO> getChannelMembers(Long channelId) {
        return memberRepository.findByCurrentChannelIdWithRoom(channelId)
                .stream()
                .map(MemberStatusDTO::from)
                .toList();
    }

    public void changeCharacterColor(String username, String colorCode) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new HttpClientErrorException(
                        HttpStatus.NOT_FOUND,
                        "존재하지 않는 회원입니다."
                ));

        if (!colorCode.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "유효하지 않은 색상 코드입니다.");
        }

        member.updateColorCode(colorCode);
        memberRepository.save(member);
    }
}
