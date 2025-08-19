document.addEventListener('DOMContentLoaded', function(){
    const form = document.getElementById('withdrawForm');
    if(!form) return;

    form.addEventListener('submit', function(e){
        e.preventDefault();

        const pw = form.querySelector('input[name="password"]').value.trim();
        const agree = form.querySelector('#agreeCheck').checked;
        const confirmText = document.getElementById('confirmText').value.trim();

        if(!pw){
            alert('현재 비밀번호를 입력하세요.');
            return;
        }
        if(!agree){
            alert('안내사항 동의가 필요합니다.');
            return;
        }
        if(confirmText !== '회원탈퇴'){
            alert('확인 문구를 정확히 입력하세요: 회원탈퇴');
            return;
        }

        const url = form.dataset.success || '/mypage/withdraw/success';
        window.location.assign(url);
    });
});
