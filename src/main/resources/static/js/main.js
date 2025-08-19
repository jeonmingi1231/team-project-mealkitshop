/* static/js/main.js */
document.addEventListener('DOMContentLoaded', () => {
    if (!document.querySelector('.banner-swiper')) return;

    new Swiper('.banner-swiper', {
        loop: true,
        autoplay: {delay: 3000, disableOnInteraction: false},
        navigation: {prevEl: '.swiper-button-prev', nextEl: '.swiper-button-next'},
        autoHeight: true
    });
});
/* --- 카테고리 토글 --- */
document.addEventListener('DOMContentLoaded', () => {
    const toggle = document.querySelector('.cat-toggle');
    const items = document.getElementById('catItems');
    if (!toggle || !items) return;

    toggle.addEventListener('click', () => {
        items.classList.toggle('show');
        const expanded = items.classList.contains('show');
        toggle.setAttribute('aria-expanded', expanded);
    });
});
/* ----- 카테고리 모바일 토글 ----- */
document.addEventListener('DOMContentLoaded', () => {
    const toggleBtn = document.getElementById('catToggle');
    const items = document.getElementById('catItems');
    if (!toggleBtn || !items) return;

    toggleBtn.addEventListener('click', () => {
        items.classList.toggle('show');
    });
});
/* ----- 카테고리 바 fixed 전환 ----- */
document.addEventListener('DOMContentLoaded', () => {
    const catBar = document.querySelector('.category-bar');
    if (!catBar) return;

    const catBarTop = catBar.offsetTop;          // 최초 위치 기억

    window.addEventListener('scroll', () => {
        if (window.scrollY >= catBarTop) {
            catBar.classList.add('fixed');
            document.body.classList.add('has-fixed-cat');
        } else {
            catBar.classList.remove('fixed');
            document.body.classList.remove('has-fixed-cat');
        }
    });
});
/* ----- 전체 카테고리 → 메가 패널 토글 ----- */
document.addEventListener('DOMContentLoaded', () => {
    const btn = document.getElementById('megaToggle');
    const panel = document.getElementById('megaPanel');
    if (btn && panel) {
        btn.addEventListener('click', () => panel.classList.toggle('open'));
    }
});
