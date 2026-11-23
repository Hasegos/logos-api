const LOGOS_LAST_READ_KEY = 'logos:last-read';

const LOGOS_READ_LOG_KEY = 'logos:read-log';

const LOGOS_READ_CHAPTERS_KEY = 'logos:read-chapters';

/**
 * localStorage에 저장된 마지막으로 읽은 책/장 정보를 가져옵니다.
 * 저장된 값이 없거나 파싱에 실패하면 null을 반환합니다.
 *
 * @returns {{book: string, chapter: string|number}|null} 마지막으로 읽은 책/장 정보
 */
function logosGetLastRead() {
    try {
        return JSON.parse(localStorage.getItem(LOGOS_LAST_READ_KEY));
    } catch (e) {
        return null;
    }
}

/**
 * 마지막으로 읽은 책/장 정보를 localStorage에 저장합니다.
 * localStorage 사용이 불가능한 환경(프라이빗 모드 등)에서는 조용히 무시합니다.
 *
 * @param {string} book - 책 이름
 * @param {string|number} chapter - 장 번호
 * @returns {void}
 */
function logosSetLastRead(book, chapter) {
    try {
        localStorage.setItem(LOGOS_LAST_READ_KEY, JSON.stringify({ book, chapter }));
    } catch (e) {
    }
}

/**
 * localStorage에 저장된 책별 읽은 장 목록 전체를 가져옵니다.
 * 저장된 값이 없거나 파싱에 실패하면 빈 객체를 반환합니다.
 *
 * @returns {Object.<string, number[]>} 책 이름을 key로, 읽은 장 번호 배열을 value로 갖는 객체
 */
function logosGetReadChapters() {
    try {
        return JSON.parse(localStorage.getItem(LOGOS_READ_CHAPTERS_KEY)) || {};
    } catch (e) {
        return {};
    }
}

/**
 * 지정한 책의 특정 장을 "읽은 장" 목록에 추가하고 localStorage에 반영합니다.
 * 이미 기록된 장이면 중복 없이 정렬된 상태로 유지됩니다.
 *
 * @param {string} book - 책 이름
 * @param {string|number} chapter - 읽음으로 표시할 장 번호
 * @returns {void}
 */
function logosMarkChapterRead(book, chapter) {
    try {
        const data = logosGetReadChapters();
        const set = new Set(data[book] || []);
        const before = set.size;
        set.add(Number(chapter));

        if (set.size === before) {
            return;
        }

        data[book] = Array.from(set).sort((a, b) => a - b);
        localStorage.setItem(LOGOS_READ_CHAPTERS_KEY, JSON.stringify(data));
        logosCountReadToday();
    } catch (e) {
    }
}

/**
 * 오늘 읽은 장 수를 하나 늘립니다.
 * 날짜별로 세어 두면 오늘 분량과 연속 일수, 읽는 속도를 계산할 수 있습니다.
 *
 * @returns {void}
 */
function logosCountReadToday() {
    try {
        const log = logosGetReadLog();
        const today = logosToday();
        log[today] = (log[today] || 0) + 1;
        localStorage.setItem(LOGOS_READ_LOG_KEY, JSON.stringify(log));
    } catch (e) {
    }
}

/**
 * 날짜별로 읽은 장 수 기록을 가져옵니다.
 *
 * @returns {Object.<string, number>} 날짜(YYYY-MM-DD)를 key로, 그날 읽은 장 수를 value로 갖는 객체
 */
function logosGetReadLog() {
    try {
        return JSON.parse(localStorage.getItem(LOGOS_READ_LOG_KEY)) || {};
    } catch (e) {
        return {};
    }
}

/**
 * 오늘 날짜를 YYYY-MM-DD 형식으로 돌려줍니다. 기기의 시간대를 그대로 씁니다.
 *
 * @param {Date} [date] 기준 날짜. 없으면 오늘
 * @returns {string} YYYY-MM-DD
 */
function logosToday(date) {
    const target = date || new Date();
    const month = String(target.getMonth() + 1).padStart(2, '0');
    const day = String(target.getDate()).padStart(2, '0');
    return `${target.getFullYear()}-${month}-${day}`;
}

/**
 * 저장된 "마지막으로 읽은 위치"와 "읽은 장 목록"을 모두 삭제합니다.
 * 초기화 버튼 클릭 시 호출됩니다.
 *
 * @returns {void}
 */
function logosClearProgress() {
    try {
        localStorage.removeItem(LOGOS_LAST_READ_KEY);
        localStorage.removeItem(LOGOS_READ_CHAPTERS_KEY);
        localStorage.removeItem(LOGOS_READ_LOG_KEY);
    } catch (e) {
    }
}

/**
 * 화면 하단에 짧은 안내 메시지를 토스트 형태로 표시했다가 일정 시간 후 사라지게 합니다.
 * 토스트 엘리먼트가 없으면 새로 생성하여 body에 추가합니다.
 *
 * @param {string} message - 토스트에 표시할 메시지
 * @returns {void}
 */
function logosShowToast(message) {
    let toast = document.getElementById('logos-toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'logos-toast';
        toast.className = 'toast';
        document.body.appendChild(toast);
    }

    toast.textContent = message;
    toast.classList.add('toast--visible');

    clearTimeout(toast._logosHideTimer);
    toast._logosHideTimer = setTimeout(() => {
        toast.classList.remove('toast--visible');
    }, 1800);
}

/**
 * 페이지 로드 시 성경읽기 관련 초기화 함수들을 순서대로 실행합니다.
 * 각 함수는 해당 페이지(책 목록 또는 장 상세)에 필요한 엘리먼트가 없으면 스스로 종료합니다.
 */
document.addEventListener('DOMContentLoaded', () => {
    initBookAccordion();
    initContinueReadingBanner();
    initReadChapterBadges();
    initChapterDetailPage();
});

/**
 * 책 목록(아코디언) 페이지에서 책 제목 클릭 시 해당 책의 장 목록을 펼치거나 접습니다.
 * 이 페이지가 아니면(해당 요소가 없으면) 아무 동작도 하지 않습니다.
 *
 * @returns {void}
 */
function initBookAccordion() {
    renderPlanBanner();

    document.querySelectorAll('.book-list__row').forEach((row) => {
        row.addEventListener('click', () => {
            const book = row.closest('.book-list__book');
            const isOpen = book.classList.toggle('book-list__book--open');
            row.querySelector('.book-list__chevron').textContent = isOpen ? '▾' : '▸';
        });
    });
}

/**
 * 책 목록 페이지 상단의 "이어서 읽기" 배너를 초기화합니다.
 * localStorage에 저장된 마지막 읽은 위치가 있으면 배너를 표시하고 링크를 채우며,
 * 배너 안의 초기화 버튼에 읽기 기록 삭제 동작을 연결합니다.
 * 저장된 기록이 없거나 배너 엘리먼트가 없으면 아무 동작도 하지 않습니다.
 *
 * @returns {void}
 */
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

/**
 * 책 목록 아코디언에서 이미 읽은 장에 체크 배지(book-list__chapter--read)를 표시합니다.
 * localStorage에 읽은 장 기록이 전혀 없으면 아무 동작도 하지 않습니다.
 *
 * @returns {void}
 */
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

/**
 * 장 상세(절 목록) 페이지를 초기화합니다.
 * - 현재 책/장을 "마지막으로 읽은 위치"와 "읽은 장 목록"에 기록합니다.
 * - 각 절 클릭 시 하이라이트를 토글하고, 해당 절의 링크를 클립보드에 복사한 뒤 토스트로 안내합니다.
 * - URL 해시(#v3 등)로 접속한 경우, 해당 절을 자동으로 하이라이트하고 화면 중앙으로 스크롤합니다.
 * 이 페이지가 아니면(책/장 정보를 가진 컨테이너가 없으면) 아무 동작도 하지 않습니다.
 *
 * @returns {void}
 */
function initChapterDetailPage() {
    const container = document.querySelector('.read[data-book]');
    if (!container) return;

    const book = container.getAttribute('data-book');
    const chapter = container.getAttribute('data-chapter');

    logosSetLastRead(book, chapter);
    logosMarkChapterRead(book, chapter);

    /**
     * 절 클릭 시 하이라이트를 토글하고, 해당 절의 공유 링크를 클립보드에 복사합니다.
     */
    document.querySelectorAll('.chapter-view__verse').forEach((verse) => {
        verse.addEventListener('click', () => {
            verse.classList.toggle('chapter-view__verse--highlighted');

            const url = `${location.origin}/read/${encodeURIComponent(book)}/${chapter}#${verse.id}`;
            navigator.clipboard.writeText(url)
                .then(() => logosShowToast('말씀 링크가 복사되었습니다'))
                .catch(() => logosShowToast('복사에 실패했습니다'));
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

/**
 * 저장된 읽기 목표를 가져옵니다.
 *
 * @returns {object|null} {days, start} 형태의 목표. 없거나 읽을 수 없으면 null
 */
function logosGetPlanGoal() {
    try {
        const goal = JSON.parse(localStorage.getItem('logos:plan-goal'));
        return goal && goal.days > 0 && goal.start ? goal : null;
    } catch (e) {
        return null;
    }
}

/**
 * 목표를 세운 사람에게만 오늘 읽어야 할 분량과 연속 일수를 배너에 덧붙입니다.
 * 전체 진도율보다 오늘 무엇을 해야 하는지가 더 자주 필요한 정보입니다.
 *
 * @param {number} remaining 아직 읽지 않은 장 수
 * @returns {void}
 */
function renderBannerToday(remaining) {
    const box = document.getElementById('plan-banner-today');
    if (!box) return;

    const goal = logosGetPlanGoal();
    if (!goal) return;

    const today = logosToday();
    const end = new Date(goal.start + 'T00:00:00');
    end.setDate(end.getDate() + goal.days);
    const daysLeft = Math.max(Math.round((end - new Date(today + 'T00:00:00')) / 86400000), 0);
    const daily = daysLeft > 0 ? Math.ceil(remaining / daysLeft) : remaining;

    const log = logosGetReadLog();
    const readToday = log[today] || 0;

    let streak = 0;
    const cursor = new Date((log[today] > 0 ? today : logosToday(new Date(Date.now() - 86400000))) + 'T00:00:00');
    while (log[logosToday(cursor)] > 0) {
        streak++;
        cursor.setDate(cursor.getDate() - 1);
    }

    box.textContent = '· 오늘 ' + readToday + '/' + daily + '장' + (streak > 0 ? ' · 🔥' + streak + '일' : '');
    box.hidden = false;
}

/**
 * 목록 페이지 위쪽에 읽기 진도 막대를 채웁니다.
 * 읽은 장이 하나도 없으면 보여주지 않습니다. 빈 막대는 알려 주는 것이 없습니다.
 *
 * @returns {void}
 */
function renderPlanBanner() {
    const banner = document.getElementById('plan-banner');
    if (!banner) return;

    const read = logosGetReadChapters();
    const readCount = Object.keys(read).reduce((sum, book) => sum + read[book].length, 0);

    if (readCount === 0 && !logosGetPlanGoal()) return;

    const total = document.querySelectorAll('.book-list__chapter').length;
    const percent = total ? readCount / total * 100 : 0;

    document.getElementById('plan-banner-fill').style.width = percent + '%';
    document.getElementById('plan-banner-value').textContent = percent.toFixed(2) + '%';
    renderBannerToday(total - readCount);
    banner.style.display = 'flex';
}
