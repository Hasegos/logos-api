document.addEventListener('DOMContentLoaded', () => {
    const links = document.querySelectorAll('.toc__link');
    const sections = Array.from(document.querySelectorAll('.docs__section'));
    const THRESHOLD = 100;
    let suppressUntil = 0;

    if (links.length === 0 || sections.length === 0) return;

    function setActive(id) {
        links.forEach((link) => {
            link.classList.toggle('toc__link--active', link.getAttribute('data-section') === id);
        });
    }

    function updateActiveLink() {
        if (Date.now() < suppressUntil) return;

        const viewportHeight = window.innerHeight;
        const atBottom = window.scrollY + viewportHeight >= document.documentElement.scrollHeight - 2;
        const effectiveThreshold = atBottom ? viewportHeight / 2 : THRESHOLD;

        let currentId = sections[0].getAttribute('data-section');
        for (const section of sections) {
            if (section.getBoundingClientRect().top <= effectiveThreshold) {
                currentId = section.getAttribute('data-section');
            } else {
                break;
            }
        }

        setActive(currentId);
    }
    links.forEach((link) => {
        link.addEventListener('click', () => {
            setActive(link.getAttribute('data-section'));
            suppressUntil = Date.now() + 700;
        });
    });

    let ticking = false;
    function onScroll() {
        if (ticking) return;
        ticking = true;
        requestAnimationFrame(() => {
            updateActiveLink();
            ticking = false;
        });
    }
    window.addEventListener('scroll', onScroll, { passive: true });
    updateActiveLink();
});