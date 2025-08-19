package org.team.mealkitshop.dto.member;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString (exclude = {"currentPassword", "newPassword", "newPasswordConfirm"})
public class MemberUpdateDTO{

        // 전화번호 변경
        @Size(max = 20)
        @Pattern(regexp = "^[0-9\\-]+$", message = "숫자와 하이픈만 입력하세요.")
        private String phone;

        // 마케팅 수신 동의 (null 허용 → 미전송 시 기존값 유지)
        private boolean marketingYn;

        // 비밀번호 변경
        @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다")
        private String currentPassword;

        @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다")
        private String newPassword;

        @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다")
        private String newPasswordConfirm;


        @AssertTrue
        public boolean isPasswordBlockValid() {
                boolean any = notBlank(currentPassword) || notBlank(newPassword) || notBlank(newPasswordConfirm);
                if (!any) return true; // 비번 변경 안 함
                return notBlank(currentPassword)
                        && notBlank(newPassword)
                        && notBlank(newPasswordConfirm)
                        && newPassword.equals(newPasswordConfirm);
        }
        private boolean notBlank(String s){ return s != null && !s.isBlank(); }
}