/**
 * 모바일 폭(640px 이하)에서 햄버거 버튼 클릭 시 상단 네비게이션 메뉴를 펼치거나 접습니다.
 * 버튼의 aria-expanded 속성도 함께 갱신하여 접근성을 보장합니다.
 */
document.addEventListener('DOMContentLoaded', () => {
    const toggle = document.getElementById('site-header-toggle');
    const nav = document.getElementById('site-header-nav');
    if (!toggle || !nav) return;

    /**
     * 햄버거 버튼 클릭 시 nav의 열림/닫힘 클래스를 토글하고,
     * 현재 상태를 aria-expanded 속성에 반영합니다.
     */
    toggle.addEventListener('click', () => {
        const isOpen = nav.classList.toggle('site-header__nav--open');
        toggle.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
    });
});

/**
 * 모바일 하단 탭에서 현재 보고 있는 메뉴를 표시합니다.
 * 탭은 모든 페이지가 쓰는 footer 조각에 있어 서버가 현재 메뉴를 모르므로 주소로 판단합니다.
 */
document.addEventListener('DOMContentLoaded', () => {
    const path = window.location.pathname;

    document.querySelectorAll('.tabbar__item').forEach((item) => {
        const href = item.getAttribute('href');
        const exactOnly = ['/', '/read/search', '/read/plan'];
        const active = exactOnly.includes(href)
            ? path === href
            : path.startsWith(href) && !exactOnly.includes(path);

        if (active) {
            item.classList.add('tabbar__item--active');
        }
    });
});