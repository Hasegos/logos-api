const LOGOS_LAST_READ_KEY = 'logos:last-read';
const LOGOS_READ_CHAPTERS_KEY = 'logos:read-chapters';

function logosGetLastRead() {
    try {
        return JSON.parse(localStorage.getItem(LOGOS_LAST_READ_KEY));
    } catch (e) {
        return null;
    }
}

function logosSetLastRead(book, chapter) {
    try {
        localStorage.setItem(LOGOS_LAST_READ_KEY, JSON.stringify({ book, chapter }));
    } catch (e) {
    }
}

function logosGetReadChapters() {
    try {
        return JSON.parse(localStorage.getItem(LOGOS_READ_CHAPTERS_KEY)) || {};
    } catch (e) {
        return {};
    }
}

function logosMarkChapterRead(book, chapter) {
    try {
        const data = logosGetReadChapters();
        const set = new Set(data[book] || []);
        set.add(Number(chapter));
        data[book] = Array.from(set).sort((a, b) => a - b);
        localStorage.setItem(LOGOS_READ_CHAPTERS_KEY, JSON.stringify(data));
    } catch (e) {
    }
}

function logosClearProgress() {
    try {
        localStorage.removeItem(LOGOS_LAST_READ_KEY);
        localStorage.removeItem(LOGOS_READ_CHAPTERS_KEY);
    } catch (e) {
    }
}

document.addEventListener('DOMContentLoaded', () => {
    initBookAccordion();
    initContinueReadingBanner();
    initReadChapterBadges();
    initChapterDetailPage();
});

function initBookAccordion() {
    document.querySelectorAll('.book-list__row').forEach((row) => {
        row.addEventListener('click', () => {
            const book = row.closest('.book-list__book');
            const isOpen = book.classList.toggle('book-list__book--open');
            row.querySelector('.book-list__chevron').textContent = isOpen ? '▾' : '▸';
        });
    });
}

function initContinueReadingBanner() {
    const banner = document.getElementById('continue-reading');
    if (!banner) return;

    const last = logosGetLastRead();
    if (!last || !last.book || !last.chapter) return;

    const link = banner.querySelector('.continue-banner__link');
    link.href = `/read/${encodeURIComponent(last.book)}/${last.chapter}`;
    link.textContent = `이어서 읽기: ${last.book} ${last.chapter}장 →`;
    banner.style.display = 'flex';

    const resetBtn = banner.querySelector('.continue-banner__reset');
    resetBtn.addEventListener('click', () => {
        const confirmed = window.confirm('읽기 기록을 초기화할까요? 이어서 읽기 위치와 읽은 장 표시가 모두 사라집니다.');
        if (!confirmed) return;

        logosClearProgress();
        banner.style.display = 'none';
        document.querySelectorAll('.book-list__chapter--read').forEach((el) => {
            el.classList.remove('book-list__chapter--read');
        });
    });
}

function initReadChapterBadges() {
    const readChapters = logosGetReadChapters();
    if (Object.keys(readChapters).length === 0) return;

    document.querySelectorAll('.book-list__book[data-book]').forEach((bookEl) => {
        const book = bookEl.getAttribute('data-book');
        const readSet = new Set(readChapters[book] || []);
        if (readSet.size === 0) return;

        bookEl.querySelectorAll('.book-list__chapter').forEach((chapterLink) => {
            if (readSet.has(Number(chapterLink.textContent.trim()))) {
                chapterLink.classList.add('book-list__chapter--read');
            }
        });
    });
}

function initChapterDetailPage() {
    const container = document.querySelector('.read[data-book]');
    if (!container) return;

    const book = container.getAttribute('data-book');
    const chapter = container.getAttribute('data-chapter');

    logosSetLastRead(book, chapter);
    logosMarkChapterRead(book, chapter);

    document.querySelectorAll('.chapter-view__verse').forEach((verse) => {
        verse.addEventListener('click', (event) => {
            if (event.target.closest('.chapter-view__verse-num')) return;
            verse.classList.toggle('chapter-view__verse--highlighted');
        });
    });

    document.querySelectorAll('.chapter-view__verse-num').forEach((numEl) => {
        numEl.addEventListener('click', (event) => {
            event.stopPropagation();
            const verseEl = numEl.closest('.chapter-view__verse');
            const url = `${location.origin}/read/${encodeURIComponent(book)}/${chapter}#${verseEl.id}`;

            navigator.clipboard.writeText(url).then(() => {
                const original = numEl.textContent;
                numEl.textContent = '✓';
                setTimeout(() => {
                    numEl.textContent = original;
                }, 1000);
            });
        });
    });

    if (location.hash) {
        try {
            const target = document.querySelector(location.hash);
            if (target && target.classList.contains('chapter-view__verse')) {
                target.classList.add('chapter-view__verse--highlighted');
                target.scrollIntoView({ block: 'center' });
            }
        } catch (e) {
        }
    }
}