package org.team.mealkitshop.controller.member;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import org.team.mealkitshop.dto.address.AddressCreateDTO;
import org.team.mealkitshop.dto.address.AddressUpdateDTO;
import org.team.mealkitshop.dto.member.MemberDetailDTO;
import org.team.mealkitshop.dto.member.MemberUpdateDTO;
import org.team.mealkitshop.service.member.MemberQueryService;
import org.team.mealkitshop.service.member.MemberService;
import org.team.mealkitshop.service.member.MemberUpdateService;


@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

    private final MemberService memberService;          // 기존 조회용
    private final MemberQueryService memberQueryService; // 내 정보 조회
    private final MemberUpdateService memberUpdateService; // ✅ 추가: 업데이트 전담 서비스

    @GetMapping
    public String index(Authentication auth, Model model) {
        if (auth == null || auth.getName() == null) return "redirect:/login";

        MemberDetailDTO memberDetail = memberService.getMemberDetail(auth.getName());
        model.addAttribute("memberDetail", memberDetail);
        model.addAttribute("couponCount", 0);      // TODO: 실제 값으로 교체
        model.addAttribute("recentOrders", null);  // TODO: 실제 값으로 교체
        return "mypage/index";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        MemberDetailDTO detail = memberQueryService.getMyInfo();
        model.addAttribute("member", detail);
        return "mypage/profile";
    }

    // 수정 페이지 GET (폼 모델 주입)
    @GetMapping("/profile/edit")
    public String profileEditForm(Model model) {
        var me = memberQueryService.getMyInfo(); // 로그인 사용자 정보
        MemberUpdateDTO form = new MemberUpdateDTO();
        form.setPhone(me.getPhone());
        form.setMarketingYn(Boolean.TRUE.equals(me.getMarketingYn())); // primitive boolean 채움
        // 비밀번호 필드는 선택 입력이라 비워둠
        model.addAttribute("form", form);
        return "mypage/profile-edit";
    }

    // ✅ 추가: 수정 POST — HTML/서비스는 그대로, 여기만 넣으면 동작
    @PostMapping("/profile/edit")
    public String profileEditSubmit(
            @AuthenticationPrincipal UserDetails user,
            @Valid @ModelAttribute("form") MemberUpdateDTO form,
            BindingResult bindingResult,
            // 배송지 신규/수정은 선택 입력 — 둘 중 하나만 들어와도 동작
            @ModelAttribute AddressCreateDTO addressCreateDto,
            @ModelAttribute AddressUpdateDTO addressUpdateDto,
            Model model
    ) {
        // 1) 폼 검증 오류 시 다시 폼 렌더링
        if (bindingResult.hasErrors()) {
            return "mypage/profile-edit";
        }

        // 2) 로그인 사용자 PK (MemberDetailDTO에 mno 존재한다고 가정)
        var me = memberQueryService.getMyInfo();
        Long memberId = me.getMno();

        // 3) 분기 — update 우선, 없으면 create, 둘 다 없으면 프로필만
        try {
            if (addressUpdateDto != null && addressUpdateDto.getId() != null) {
                memberUpdateService.updateProfile(memberId, form, addressUpdateDto);
            } else if (isCreatePayload(addressCreateDto)) {
                memberUpdateService.updateProfile(memberId, form, addressCreateDto);
            } else {
                memberUpdateService.updateProfile(memberId, form);
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            // 전역 에러로 바인딩해 템플릿 상단에 노출
            bindingResult.reject("profileEdit.failed", ex.getMessage());
            return "mypage/profile-edit";
        }

        return "redirect:/mypage/profile/success";
    }

    @GetMapping("/profile/success")
    public String profileEditSuccess() {
        return "mypage/profile-edit-success";
    }

    @GetMapping("/withdraw")
    public String withdrawPage() { return "mypage/withdraw"; }

    

    @GetMapping("/withdraw/success")
    public String withdrawSuccess() { return "mypage/withdraw-success"; }

    // 신규 주소 payload 유무 간단 판정
    private boolean isCreatePayload(AddressCreateDTO dto) {
        if (dto == null) return false;
        return (notBlank(dto.getZipCode()) || notBlank(dto.getAddr1())
                || notBlank(dto.getAlias()) || notBlank(dto.getAddr2())
                || (dto.getIsDefault() != null && dto.getIsDefault()));
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
