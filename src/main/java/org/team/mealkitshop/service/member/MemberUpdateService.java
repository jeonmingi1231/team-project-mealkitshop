package org.team.mealkitshop.service.member;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.Provider;
import org.team.mealkitshop.common.Status;
import org.team.mealkitshop.domain.member.Address;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.address.AddressCreateDTO;
import org.team.mealkitshop.dto.address.AddressUpdateDTO;
import org.team.mealkitshop.dto.member.MemberDeleteDTO;
import org.team.mealkitshop.dto.member.MemberUpdateDTO;
import org.team.mealkitshop.repository.address.AddressRepository;
import org.team.mealkitshop.repository.member.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberUpdateService {

    private final MemberRepository memberRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;

    /* -----------------------------
     * 오버로드: 상황에 맞게 골라 호출
     * ----------------------------- */

    /** 프로필만 (배송지 미변경) */
    @Transactional
    public void updateProfile(Long memberId, MemberUpdateDTO memberDto) {
        doUpdateProfile(memberId, memberDto, null, null);
    }

    /** 프로필 + 배송지 신규 생성 */
    @Transactional
    public void updateProfile(Long memberId, MemberUpdateDTO memberDto, AddressCreateDTO addressCreateDto) {
        doUpdateProfile(memberId, memberDto, addressCreateDto, null);
    }

    /** 프로필 + 배송지 수정 */
    @Transactional
    public void updateProfile(Long memberId, MemberUpdateDTO memberDto, AddressUpdateDTO addressUpdateDto) {
        doUpdateProfile(memberId, memberDto, null, addressUpdateDto);
    }

    /** 프로필 + 배송지(신규/수정 중 하나 선택) */
    @Transactional
    public void updateProfile(Long memberId,
                              MemberUpdateDTO memberDto,
                              AddressCreateDTO createDto,
                              AddressUpdateDTO updateDto) {
        doUpdateProfile(memberId, memberDto, createDto, updateDto);
    }

    /* -----------------------------
     * 공통 내부 처리
     * ----------------------------- */
    private void doUpdateProfile(Long memberId,
                                 MemberUpdateDTO memberDto,
                                 AddressCreateDTO createDto,     // null 또는 유효값
                                 AddressUpdateDTO updateDto) {   // null 또는 유효값

        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        // (1) 전화번호
        if (notBlank(memberDto.getPhone())) {
            m.setPhone(memberDto.getPhone());
        }

        // (2) 마케팅 동의
        // primitive boolean -> 폼에서 미체크면 false가 넘어옴 (즉, 항상 반영)
        m.setMarketingYn(memberDto.isMarketingYn());

        // (3) 비밀번호 변경
        if (anyPasswordFieldFilled(memberDto)) {
            validatePasswordBlock(memberDto); // 세 칸 입력 + 새=확인 + (선택) 현재와 다른지
            if (!passwordEncoder.matches(memberDto.getCurrentPassword(), m.getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }
            m.setPassword(passwordEncoder.encode(memberDto.getNewPassword()));
        }

        // (4) 배송지 처리: update 우선, 없으면 create
        if (updateDto != null) {
            updateAddress(memberId, updateDto);
        } else if (createDto != null) {
            createAddress(memberId, m, createDto);
        }
    }

    /* -----------------------------
     * 배송지 보조 메서드
     * ----------------------------- */
    private void updateAddress(Long memberId, AddressUpdateDTO dto) {
        Address a = addressRepository.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("주소가 존재하지 않습니다."));
        if (!a.getMember().getMno().equals(memberId)) {
            throw new IllegalStateException("본인 주소만 수정할 수 있습니다.");
        }

        if (dto.getAlias()   != null) a.setAlias(dto.getAlias());
        if (dto.getZipCode() != null) a.setZipCode(dto.getZipCode());
        if (dto.getAddr1()   != null) a.setAddr1(dto.getAddr1());
        if (dto.getAddr2()   != null) a.setAddr2(dto.getAddr2());

        if (dto.getIsDefault() != null) {
            a.setDefault(dto.getIsDefault());
        }

        addressRepository.save(a);

        // 기본배송지 유일성 보장
        if (Boolean.TRUE.equals(a.isDefault())) {
            addressRepository.clearDefaultExcept(memberId, a.getAddressId());
        }
    }

    private void createAddress(Long memberId, Member m, AddressCreateDTO dto) {
        Address a = new Address();
        a.setMember(m);
        a.setAlias(dto.getAlias());
        a.setZipCode(dto.getZipCode());
        a.setAddr1(dto.getAddr1());
        a.setAddr2(dto.getAddr2());
        a.setDefault(Boolean.TRUE.equals(dto.getIsDefault()));

        addressRepository.save(a);

        // 기본배송지 유일성 보장
        if (a.isDefault()) {
            addressRepository.clearDefaultExcept(memberId, a.getAddressId());
        }
    }

    /* -----------------------------
     * 탈퇴(기존 로직 유지)
     * ----------------------------- */
    @Transactional
    public void withdraw(Long memberId, MemberDeleteDTO dto) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        if (m.getProvider() == Provider.Local) {
            if (dto == null || !notBlank(dto.getCurrentPassword())) {
                throw new IllegalArgumentException("현재 비밀번호를 입력하세요.");
            }
            if (!passwordEncoder.matches(dto.getCurrentPassword(), m.getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }
        }

        m.setStatus(Status.WITHDRAWN);
        m.setMarketingYn(false);

        String suffix = "_withdrawn_" + m.getMno();
        m.setEmail(m.getEmail() + suffix);
        m.setMemberName(m.getMemberName() + suffix);
    }

    /* -----------------------------
     * 공통 유틸
     * ----------------------------- */
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private boolean anyPasswordFieldFilled(MemberUpdateDTO d) {
        return notBlank(d.getCurrentPassword())
                || notBlank(d.getNewPassword())
                || notBlank(d.getNewPasswordConfirm());
    }

    private void validatePasswordBlock(MemberUpdateDTO d) {
        if (!notBlank(d.getCurrentPassword())
                || !notBlank(d.getNewPassword())
                || !notBlank(d.getNewPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호 변경은 세 칸 모두 입력해야 합니다.");
        }
        if (!d.getNewPassword().equals(d.getNewPasswordConfirm())) {
            throw new IllegalArgumentException("새 비밀번호가 서로 일치하지 않습니다.");
        }
        if (d.getCurrentPassword().equals(d.getNewPassword())) {
            throw new IllegalArgumentException("현재 비밀번호와 다른 새 비밀번호를 입력하세요.");
        }
        // 필요 시 추가 정책(길이/조합/재사용 금지 등) 여기에 확장
    }
}
