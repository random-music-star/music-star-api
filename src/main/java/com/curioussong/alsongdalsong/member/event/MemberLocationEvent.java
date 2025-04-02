package com.curioussong.alsongdalsong.member.event;

import com.curioussong.alsongdalsong.member.domain.Member;

public record MemberLocationEvent(Long channelId, Member member) {
}
