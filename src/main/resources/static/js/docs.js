/**
 * API 문서 페이지의 좌측 목차(TOC)를 스크롤 위치에 따라 자동으로 하이라이트합니다.
 * 목차 항목 클릭 시에는 스크롤 계산 대신 즉시 해당 항목을 활성화합니다.
 */
document.addEventListener('DOMContentLoaded', () => {
    const links = document.querySelectorAll('.toc__link');
    const sections = Array.from(document.querySelectorAll('.docs__section'));
    const THRESHOLD = 100;
    let suppressUntil = 0;

    if (links.length === 0 || sections.length === 0) return;

    /**
     * 주어진 섹션 id에 해당하는 목차 링크만 활성(active) 상태로 표시합니다.
     *
     * @param {string} id - 활성화할 섹션의 data-section 값
     * @returns {void}
     */
    function setActive(id) {
        links.forEach((link) => {
            link.classList.toggle('toc__link--active', link.getAttribute('data-section') === id);
        });
    }

    /**
     * 현재 스크롤 위치를 기준으로 활성화할 섹션을 계산하고 목차에 반영합니다.
     * 페이지 맨 아래 근처에서는 마지막 섹션들이 짧아 기준선(THRESHOLD)에 못 미칠 수 있으므로
     * 기준을 화면 절반(viewportHeight / 2)까지 완화합니다.
     * 클릭 직후 일정 시간(suppressUntil) 동안은 스크롤에 의한 갱신을 건너뜁니다.
     *
     * @returns {void}
     */
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

    /**
     * 목차 링크 클릭 시, 스크롤 이동이 끝나기 전에도 즉시 해당 항목을 활성화하고
     * 짧은 시간 동안 스크롤 기반 갱신(updateActiveLink)을 일시 중단합니다.
     */
    links.forEach((link) => {
        link.addEventListener('click', () => {
            setActive(link.getAttribute('data-section'));
            suppressUntil = Date.now() + 700;
        });
    });

    let ticking = false;

    /**
     * 스크롤 이벤트를 requestAnimationFrame으로 쓰로틀링하여
     * 프레임당 한 번만 updateActiveLink가 실행되도록 합니다.
     *
     * @returns {void}
     */
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