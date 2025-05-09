package com.curioussong.alsongdalsong.channel.service;

import com.curioussong.alsongdalsong.channel.domain.Channel;
import com.curioussong.alsongdalsong.channel.dto.ChannelResponse;
import com.curioussong.alsongdalsong.channel.enums.ChannelType;
import com.curioussong.alsongdalsong.channel.event.ChannelChatSaveEvent;
import com.curioussong.alsongdalsong.channel.repository.ChannelRepository;
import com.curioussong.alsongdalsong.common.error.stomperror.StompError;
import com.curioussong.alsongdalsong.common.error.stomperror.StompException;
import com.curioussong.alsongdalsong.common.sse.SseEmitterManager;
import com.curioussong.alsongdalsong.common.util.BadWordFilter;
import com.curioussong.alsongdalsong.common.util.Destination;
import com.curioussong.alsongdalsong.game.dto.chat.ChatRequestDTO;
import com.curioussong.alsongdalsong.game.messaging.GameMessageSender;
import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.repository.MemberRepository;
import com.curioussong.alsongdalsong.stomp.SessionManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {
    private final ChannelRepository channelRepository;
    private final MemberRepository memberRepository;
    private final SessionManager sessionManager;
    private final SseEmitterManager sseEmitterManager;
    private final GameMessageSender gameMessageSender;
    private final ApplicationEventPublisher eventPublisher;

    public List<ChannelResponse> getAllChannels() {
        List<Channel> channels = channelRepository.findAll();

        return channels.stream()
                .map(channel -> {
                    ChannelType channelType = ChannelType.getById(channel.getId());
                    int totalPlayers = memberRepository.countMembersByChannelId(channel.getId());

                    return ChannelResponse.builder()
                            .channelId(channel.getId())
                            .name(channelType.name())
                            .playerCount(totalPlayers)
                            .maxPlayers(channel.getMaxUsers())
                            .build();
                })
                .toList();
    }

    public void notifyChannelUpdate(Long channelId){
        try{
            ChannelResponse channelResponse = getChannelById(channelId);
            sseEmitterManager.sendToChannelSelectors("CHANNEL_UPDATE", channelResponse);
            log.debug("[SSE] 채널 상태 알림 전송 완료: channelId={}, playerCount={}", channelId, channelResponse.playerCount());
        } catch (EntityNotFoundException e) {
            log.warn("[SSE] 존재하지 않는 채널 요청: channelId={}, message={}", channelId, e.getMessage());
        } catch (Exception e) {
            log.error("[SSE] 채널 상태 알림 실패: channelId={}, error={}", channelId, e.getMessage(), e);
        }
    }

    private ChannelResponse getChannelById(Long channelId) {
        // 채널 정보 조회
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 채널입니다."));

        // 해당 채널 타입 조회
        ChannelType channelType = ChannelType.getById(channelId);

        // 채널 접속자 수 조회
        int playerCount = sessionManager.getChannelUserCount(channelId);

        // DTO 구성 및 반환
        return ChannelResponse.builder()
                .channelId(channelId)
                .name(channelType.name())
                .playerCount(playerCount)
                .maxPlayers(channel.getMaxUsers())
                .build();
    }

    public void channelChatMessage(ChatRequestDTO chatRequestDTO, Long channelId) {
        String destination = Destination.channel(channelId);
        String filteredMessage = BadWordFilter.filter(chatRequestDTO.getRequest().getMessage());
        chatRequestDTO.getRequest().setMessage(filteredMessage);
        gameMessageSender.sendChat(chatRequestDTO, destination);

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new StompException(StompError.CHANNEL_NOT_FOUND));
        Member member = memberRepository.findByUsername(chatRequestDTO.getRequest().getSender())
                .orElseThrow(() -> new StompException(StompError.USER_NOT_FOUND));

//        eventPublisher.publishEvent(new ChannelChatSaveEvent(member, channel, chatRequestDTO.getRequest().getMessage(), LocalDateTime.now()));
    }
}
