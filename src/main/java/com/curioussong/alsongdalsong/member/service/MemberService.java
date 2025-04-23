package com.curioussong.alsongdalsong.member.service;

import com.curioussong.alsongdalsong.channel.domain.Channel;
import com.curioussong.alsongdalsong.channel.repository.ChannelRepository;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.dto.MemberStatusDTO;
import com.curioussong.alsongdalsong.member.repository.MemberRepository;
import com.curioussong.alsongdalsong.member.service.external.ToxicUserName;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final ToxicUserName toxicUserName;

    public Member getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        log.debug("current username: {}", username);
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.UNAUTHORIZED));
    }

    public  Member getMemberByToken(String token) {
        return memberRepository.findByUsername(token).orElseThrow(() -> new HttpClientErrorException(
                HttpStatus.NOT_FOUND,
                "해당 회원이 없습니다."
        ));
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
