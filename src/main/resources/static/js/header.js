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