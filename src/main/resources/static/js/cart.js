// cart.js v2 — 선택 주문/선택 삭제만 담당 (합계는 서버값 사용)
(function(){
    const checks = Array.from(document.querySelectorAll('.item-check'));
    const btnOrderSelected = document.getElementById('btnOrderSelected');
    const orderSelectedIds = document.getElementById('orderSelectedIds');

    const removeForm = document.getElementById('removeForm');
    const removeIds = document.getElementById('removeIds');

    // 선택 여부에 따라 버튼 활성/비활성
    const updateButtons = () => {
        const anySelected = checks.some(c => c.checked);
        if(btnOrderSelected) btnOrderSelected.disabled = !anySelected;
    };

    // 선택 주문: hidden에 cartItemId 콤마로 채움
    if(btnOrderSelected && orderSelectedIds){
        btnOrderSelected.closest('form').addEventListener('submit', (e)=>{
            const ids = checks.filter(c=>c.checked).map(c=>c.value);
            if(ids.length === 0){
                e.preventDefault();
                alert('주문할 상품을 선택하세요.');
                return;
            }
            orderSelectedIds.value = ids.join(',');
        });
    }

    // 선택 삭제: hidden에 cartItemId 콤마로 채움
    removeForm?.addEventListener('submit', (e)=>{
        const ids = checks.filter(c=>c.checked).map(c=>c.value);
        if(ids.length === 0){
            e.preventDefault();
            alert('삭제할 상품을 선택하세요.');
            return;
        }
        removeIds.value = ids.join(',');
    });

    // 바인딩
    checks.forEach(c => c.addEventListener('change', updateButtons));
    updateButtons();

    // 수량 입력 직접 수정 시 최소값 보정(서버 검증과 중복 방지)
    document.querySelectorAll('.qty-form input[name="quantity"]').forEach(inp=>{
        inp.addEventListener('change', ()=>{
            if(!inp.value || +inp.value < 1) inp.value = 1;
        });
    });
})();
