package org.team.mealkitshop.service.member;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.common.Provider;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.common.Status;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.member.MemberDetailDTO;
import org.team.mealkitshop.repository.member.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder; // BCrypt

    /** 회원 저장: 이메일 정규화 + 중복검사 + 기본값 + 비밀번호 인코딩(1회) */
    @Transactional
    public Member saveMember(Member member) {
        // 1) 이메일 정규화 후 중복검사
        String normalizedEmail = normalize(member.getEmail());
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("이메일이 비어있습니다.");
        }
        if (memberRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("이미 가입된 회원입니다.");
        }
        member.setEmail(normalizedEmail);

        // 2) 비밀번호 인코딩 (이미 bcrypt면 재인코딩 금지)
        String raw = member.getPassword();
        if (raw != null && !isBcryptHash(raw)) {
            member.setPassword(passwordEncoder.encode(raw));
        }

        // 3) 기본값 세팅
        if (member.getProvider() == null) member.setProvider(Provider.Local);
        if (member.getRole() == null) member.setRole(Role.USER);
        if (member.getStatus() == null) member.setStatus(Status.ACTIVE);
        if (member.getGrade() == null) member.setGrade(Grade.BASIC);
        if (member.getPoints() == null) member.setPoints(0);

        // 4) 저장
        return memberRepository.save(member);
    }

    /** 이메일 존재 여부 (정규화 적용) */
    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(normalize(email));
    }

    /** 회원 상세 조회 DTO */
    public MemberDetailDTO getMemberDetail(String email) {
        String normalizedEmail = normalize(email);
        Member m = memberRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("회원이 존재하지 않습니다: " + normalizedEmail));

        return MemberDetailDTO.builder()
                .mno(m.getMno())
                .email(m.getEmail())
                .memberName(m.getMemberName()) // 필드명이 memberName인 엔티티 기준
                .phone(m.getPhone())
                .grade(m.getGrade())
                .points(m.getPoints() == null ? 0 : m.getPoints())
                // 주소 매핑이 필요하면 아래 주석 참고
                // .addresses(m.getAddresses().stream().map(a -> {
                //     MemberDetailDTO.AddressDTO dto = new MemberDetailDTO.AddressDTO();
                //     dto.setId(a.getId());
                //     dto.setAlias(a.getAlias());
                //     dto.setZipCode(a.getZipCode());
                //     dto.setAddr1(a.getAddr1());
                //     dto.setAddr2(a.getAddr2());
                //     dto.setIsDefault(a.getIsDefault());
                //     return dto;
                // }).toList())
                .build();
    }

    /** 스프링 시큐리티 로그인용 */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = normalize(username); // 정규화해서 조회
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("회원이 존재하지 않습니다: " + email));

        boolean disabled = member.getStatus() == Status.WITHDRAWN;
        boolean accountLocked = false;
        boolean accountExpired = false;
        boolean credentialsExpired = false;

        return User.builder()
                .username(member.getEmail())
                .password(member.getPassword())
                .roles(member.getRole().name()) // "USER" -> ROLE_USER 자동
                .disabled(disabled)
                .accountLocked(accountLocked)
                .accountExpired(accountExpired)
                .credentialsExpired(credentialsExpired)
                .build();
    }

    /** 소문자/트림 정규화 */
    private String normalize(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }

    /** bcrypt 해시 패턴 검증 (2a/2b/2y, cost 2자리, 53자 본문) */
    private boolean isBcryptHash(String v) {
        return v != null && v.matches("\\A\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}\\z");
    }
}
