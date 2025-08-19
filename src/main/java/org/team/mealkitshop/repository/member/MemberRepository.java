package org.team.mealkitshop.repository.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.team.mealkitshop.domain.member.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // email 중복 체크
    boolean existsByEmail(String email);

    // email로 회원 조회
    Optional<Member> findByEmail(String email);

    // 닉네임(=memberName) 중복 체크
    boolean existsByMemberName(String memberName);


}

