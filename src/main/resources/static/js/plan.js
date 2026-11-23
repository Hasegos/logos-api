/**
 * 읽기 진도 페이지를 그립니다.
 * 읽은 장 기록은 브라우저에만 있으므로 서버에 묻지 않고 localStorage 에서 읽습니다.
 */
(function () {
    const root = document.getElementById('plan');
    if (!root) {
        return;
    }

    const ORDER_KEY = 'logos:plan-order';

    const GOAL_KEY = 'logos:plan-goal';

    const SPEED_WINDOW_DAYS = 7;

    const total = Number(root.dataset.total);
    const bookRows = Array.from(document.querySelectorAll('.plan-book'));
    let hideDone = false;

    /**
     * 저장된 읽는 순서를 가져옵니다.
     *
     * @returns {string} 'canon' 또는 'newFirst'
     */
    function loadOrder() {
        try {
            return localStorage.getItem(ORDER_KEY) === 'newFirst' ? 'newFirst' : 'canon';
        } catch (e) {
            return 'canon';
        }
    }

    /**
     * 읽는 순서를 저장합니다.
     *
     * @param order 'canon' 또는 'newFirst'
     */
    function saveOrder(order) {
        try {
            localStorage.setItem(ORDER_KEY, order);
        } catch (e) {
            /* 저장하지 못해도 화면은 그대로 그린다 */
        }
    }

    /**
     * 저장된 목표를 가져옵니다.
     *
     * @returns {object|null} {days, start} 형태의 목표. 없으면 null
     */
    function loadGoal() {
        try {
            const goal = JSON.parse(localStorage.getItem(GOAL_KEY));
            return goal && goal.days > 0 && goal.start ? goal : null;
        } catch (e) {
            return null;
        }
    }

    /**
     * 목표를 저장합니다.
     *
     * @param goal 저장할 목표. null이면 목표를 지웁니다
     */
    function saveGoal(goal) {
        try {
            if (goal === null) {
                localStorage.removeItem(GOAL_KEY);
            } else {
                localStorage.setItem(GOAL_KEY, JSON.stringify(goal));
            }
        } catch (e) {
            /* 저장하지 못해도 화면은 그대로 그린다 */
        }
    }

    /**
     * 날짜에 일수를 더한 날짜 문자열을 돌려줍니다.
     *
     * @param dateText 기준 날짜 (YYYY-MM-DD)
     * @param days     더할 일수
     * @returns {string} YYYY-MM-DD
     */
    function addDays(dateText, days) {
        const date = new Date(dateText + 'T00:00:00');
        date.setDate(date.getDate() + days);
        return logosToday(date);
    }

    /**
     * 두 날짜 사이의 일수를 셉니다.
     *
     * @param from 시작 날짜 (YYYY-MM-DD)
     * @param to   끝 날짜 (YYYY-MM-DD)
     * @returns {number} 지난 일수
     */
    function daysBetween(from, to) {
        const start = new Date(from + 'T00:00:00');
        const end = new Date(to + 'T00:00:00');
        return Math.round((end - start) / 86400000);
    }

    /**
     * 선택한 순서대로 책 목록을 돌려줍니다.
     * 신약 먼저를 고르면 신약 책을 앞에 둡니다.
     *
     * @param order 'canon' 또는 'newFirst'
     * @returns {HTMLElement[]} 순서대로 정렬된 책 줄
     */
    function orderedBooks(order) {
        if (order !== 'newFirst') {
            return bookRows;
        }
        const isNew = (row) => row.dataset.testament === '신약';
        return bookRows.filter(isNew).concat(bookRows.filter((row) => !isNew(row)));
    }

    /**
     * 아직 읽지 않은 첫 장을 찾습니다.
     *
     * @param order 읽는 순서
     * @param read  책별 읽은 장 목록
     * @returns {object|null} 다음 읽을 책과 장. 모두 읽었으면 null
     */
    function findNext(order, read) {
        for (const row of orderedBooks(order)) {
            const chapters = Number(row.dataset.chapters);
            const done = new Set(read[row.dataset.book] || []);

            for (let chapter = 1; chapter <= chapters; chapter++) {
                if (!done.has(chapter)) {
                    return { book: row.dataset.book, chapter: chapter };
                }
            }
        }
        return null;
    }

    /**
     * 오늘을 포함해 며칠 연속으로 읽었는지 셉니다.
     * 오늘 아직 안 읽었으면 어제까지의 기록으로 셉니다. 하루가 끝나기 전에 연속이 끊겼다고 알리지 않기 위해서입니다.
     *
     * @param log 날짜별 읽은 장 수
     * @returns {number} 연속 일수
     */
    function countStreak(log) {
        const today = logosToday();
        let cursor = log[today] > 0 ? today : addDays(today, -1);
        let streak = 0;

        while (log[cursor] > 0) {
            streak++;
            cursor = addDays(cursor, -1);
        }
        return streak;
    }

    /**
     * 최근 기록으로 하루 평균 읽은 장 수를 구합니다.
     *
     * @param log 날짜별 읽은 장 수
     * @returns {number} 하루 평균 장 수. 기록이 없으면 0
     */
    function recentSpeed(log) {
        const today = logosToday();
        let sum = 0;

        for (let i = 0; i < SPEED_WINDOW_DAYS; i++) {
            sum += log[addDays(today, -i)] || 0;
        }
        return sum / SPEED_WINDOW_DAYS;
    }

    /**
     * 목표 영역을 그립니다. 목표가 없으면 감춥니다.
     *
     * @param readCount 지금까지 읽은 장 수
     * @param log       날짜별 읽은 장 수
     */
    function renderGoal(readCount, log) {
        const box = document.getElementById('plan-goal');
        const goal = loadGoal();
        const today = logosToday();
        const remaining = Math.max(total - readCount, 0);

        if (!goal) {
            box.hidden = true;
            return;
        }

        box.hidden = false;

        const endDate = addDays(goal.start, goal.days);
        const daysLeft = Math.max(daysBetween(today, endDate), 0);
        const daily = daysLeft > 0 ? Math.ceil(remaining / daysLeft) : remaining;
        const readToday = log[today] || 0;
        const ratio = daily > 0 ? Math.min(readToday / daily * 100, 100) : 100;

        document.getElementById('plan-goal-date').textContent = endDate;
        document.getElementById('plan-goal-daily').textContent = daily;
        document.getElementById('plan-goal-left').textContent = daysLeft;
        document.getElementById('plan-today').textContent = readToday;
        document.getElementById('plan-today-goal').textContent = daily;
        document.getElementById('plan-today-fill').style.width = ratio + '%';

        const streak = countStreak(log);
        document.getElementById('plan-streak').textContent = streak > 0 ? '🔥 ' + streak + '일 연속' : '연속 기록 없음';

        const speed = recentSpeed(log);
        const forecast = document.getElementById('plan-forecast');
        if (remaining === 0) {
            forecast.textContent = '성경을 모두 읽었습니다';
        } else if (speed > 0) {
            forecast.textContent = '이 속도면 ' + addDays(today, Math.ceil(remaining / speed)) + ' 완독';
        } else {
            forecast.textContent = '최근 7일 기록이 없어 완독 예상일을 낼 수 없습니다';
        }
    }

    /**
     * 진도율과 책별 막대, 다음 읽을 장을 다시 그립니다.
     */
    function render() {
        const read = typeof logosGetReadChapters === 'function' ? logosGetReadChapters() : {};
        const log = typeof logosGetReadLog === 'function' ? logosGetReadLog() : {};
        const order = loadOrder();
        const goal = loadGoal();
        const testament = { '구약': { done: 0, total: 0 }, '신약': { done: 0, total: 0 } };
        let readCount = 0;

        bookRows.forEach((row) => {
            const chapters = Number(row.dataset.chapters);
            const done = (read[row.dataset.book] || [])
                .filter((chapter) => chapter >= 1 && chapter <= chapters).length;
            readCount += done;

            const group = testament[row.dataset.testament];
            if (group) {
                group.done += done;
                group.total += chapters;
            }

            row.querySelector('.plan-book__fill').style.width = chapters ? (done / chapters * 100) + '%' : '0';
            row.querySelector('.plan-book__count').firstChild.textContent = done + ' / ';

            const finished = chapters > 0 && done === chapters;
            row.classList.toggle('plan-book--done', finished);
            row.hidden = hideDone && finished;
        });

        const percent = total ? readCount / total * 100 : 0;
        document.getElementById('plan-fill').style.width = percent + '%';
        document.getElementById('plan-read').textContent = readCount;
        document.getElementById('plan-percent').textContent = percent.toFixed(2);
        document.getElementById('plan-old').textContent = testament['구약'].done + ' / ' + testament['구약'].total;
        document.getElementById('plan-new').textContent = testament['신약'].done + ' / ' + testament['신약'].total;

        document.querySelectorAll('[data-order]').forEach((button) => {
            button.classList.toggle('plan-order__option--active', button.dataset.order === order);
        });
        document.querySelectorAll('[data-goal]').forEach((button) => {
            const value = button.dataset.goal;
            const active = goal ? String(goal.days) === value || (value === 'custom' && !['365', '100'].includes(String(goal.days)))
                : value === 'none';
            button.classList.toggle('plan-order__option--active', active);
        });

        document.getElementById('plan-hide-done').textContent = hideDone ? '다 읽은 책 보기' : '다 읽은 책 숨기기';

        renderGoal(readCount, log);

        const next = findNext(order, read);
        const nextLink = document.getElementById('plan-next');
        if (next) {
            nextLink.href = '/read/' + encodeURIComponent(next.book) + '/' + next.chapter;
            nextLink.textContent = '다음 읽을 장: ' + next.book + ' ' + next.chapter + '장 →';
        } else {
            nextLink.href = '/read';
            nextLink.textContent = '성경을 모두 읽었습니다';
        }
    }

    document.querySelectorAll('[data-order]').forEach((button) => {
        button.addEventListener('click', () => {
            saveOrder(button.dataset.order);
            render();
        });
    });

    document.querySelectorAll('[data-goal]').forEach((button) => {
        button.addEventListener('click', () => {
            const value = button.dataset.goal;

            if (value === 'none') {
                saveGoal(null);
            } else if (value === 'custom') {
                const answer = window.prompt('며칠 안에 다 읽을까요? (숫자)', '180');
                const days = Number(answer);
                if (!days || days < 1) {
                    return;
                }
                saveGoal({ days: Math.floor(days), start: logosToday() });
            } else {
                saveGoal({ days: Number(value), start: logosToday() });
            }
            render();
        });
    });

    document.getElementById('plan-hide-done').addEventListener('click', () => {
        hideDone = !hideDone;
        render();
    });

    document.getElementById('plan-reset').addEventListener('click', () => {
        if (!window.confirm('읽은 기록과 날짜별 기록을 모두 지웁니다. 되돌릴 수 없습니다. 초기화할까요?')) {
            return;
        }
        if (typeof logosClearProgress === 'function') {
            logosClearProgress();
        }
        render();
    });

    render();
})();
