package org.team.mealkitshop.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.Optional;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final MemberRepository memberRepository;

    public record LoginMemberSummary(String name, Grade grade) {}

    @ModelAttribute("loginMember")
    public LoginMemberSummary addLoginMemberToModel() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        String email = auth.getName(); // UserDetails.username = 이메일
        Optional<Member> opt = memberRepository.findByEmail(email);
        return opt.map(m -> new LoginMemberSummary(m.getMemberName(), m.getGrade())).orElse(null);
    }
}
