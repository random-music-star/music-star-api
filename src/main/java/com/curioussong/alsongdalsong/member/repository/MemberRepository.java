package com.curioussong.alsongdalsong.member.repository;

import com.curioussong.alsongdalsong.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByUsername(String username);
    Optional<Member> findByUsername(String username);
    Optional<Member> findById(Long id);
}
