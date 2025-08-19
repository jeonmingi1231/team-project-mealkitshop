// /static/js/mypage-profile-edit.js
document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('profileEditForm');

    // 1) 폼 제출 가로채서 완료 페이지로 이동 (백엔드 저장 없이)
    if (form) {
        // form.addEventListener('submit', function (e) {
        //     e.preventDefault(); // 실제 POST 방지
        //     const url =
        //         form.dataset.success ||
        //         form.getAttribute('data-success') ||
        //         '/mypage/profile/success';
        //     window.location.assign(url);
        // });
    }

    // 2) 휴대폰 입력 보정: 숫자/하이픈만, 최대 20자
    const phone =
        (form && form.querySelector('input[name="phone"]')) ||
        document.querySelector('input[name="phone"]');
    if (phone) {
        phone.addEventListener('input', () => {
            phone.value = phone.value.replace(/[^\d-]/g, '').slice(0, 20);
        });
    }

    // 3) 비밀번호 일치 힌트 (프론트 UX 보조)
    const pwNew =
        (form && form.querySelector('input[name="newPassword"]')) ||
        document.querySelector('input[name="newPassword"]');
    const pwConfirm =
        (form && form.querySelector('input[name="newPasswordConfirm"]')) ||
        document.querySelector('input[name="newPasswordConfirm"]');

    if (pwNew && pwConfirm) {
        let hint = document.createElement('div');
        hint.className = 'pf-help mt-1';
        pwConfirm.parentElement.appendChild(hint);

        const update = () => {
            const a = (pwNew.value || '').trim();
            const b = (pwConfirm.value || '').trim();
            hint.textContent =
                !a && !b
                    ? ''
                    : a === b
                        ? '새 비밀번호가 일치합니다.'
                        : '새 비밀번호가 일치하지 않습니다.';
        };

        pwNew.addEventListener('input', update);
        pwConfirm.addEventListener('input', update);
    }
});
