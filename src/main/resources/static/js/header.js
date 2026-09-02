document.addEventListener('DOMContentLoaded', () => {
    const toggle = document.getElementById('site-header-toggle');
    const nav = document.getElementById('site-header-nav');
    if (!toggle || !nav) return;

    toggle.addEventListener('click', () => {
        const isOpen = nav.classList.toggle('site-header__nav--open');
        toggle.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
    });
});