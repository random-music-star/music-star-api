package com.curioussong.alsongdalsong.member.repository;

import com.curioussong.alsongdalsong.member.domain.Member;
import com.curioussong.alsongdalsong.member.dto.MemberStatusDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByUsername(String username);
    Optional<Member> findByUsername(String username);
    Optional<Member> findById(Long id);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.currentChannel.id = :channelId")
    int countMembersByChannelId(@Param("channelId") Long channelId);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.room WHERE m.currentChannel.id = :channelId")
    List<Member> findByCurrentChannelIdWithRoom(@Param("channelId") Long channelId);
}
