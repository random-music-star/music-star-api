package com.curioussong.alsongdalsong.member.repository;

import com.curioussong.alsongdalsong.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByUsername(String username);
    Member findByUsername(String username);
}
