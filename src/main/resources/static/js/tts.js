/**
 * 장 페이지의 본문을 절 단위로 읽어 주는 낭독 기능입니다.
 * 브라우저의 음성 합성(speechSynthesis)을 쓰므로 서버를 거치지 않습니다.
 * <p>
 * 장이 끝나면 페이지를 새로 열지 않고 다음 장 내용만 바꿔 끼웁니다.
 * 페이지를 이동하면 브라우저가 사용자가 누른 적 없는 소리로 보고 막기 때문입니다.
 * </p>
 */
(function () {
    const synth = window.speechSynthesis;
    if (!synth || !document.querySelector('.read[data-book]')) {
        return;
    }

    const TTS_KEY = 'logos:tts';

    let verseIndex = 0;
    let reading = false;
    let speaking = null;

    /**
     * 지금 화면에 있는 장 정보를 읽습니다. 장을 바꿔 끼우므로 매번 새로 조회합니다.
     *
     * @returns {object} 본문 요소와 책 · 장 정보
     */
    function current() {
        const view = document.querySelector('.read[data-book]');
        return {
            view: view,
            book: view.dataset.book,
            chapter: Number(view.dataset.chapter),
            nextBook: view.dataset.nextBook || '',
            nextChapter: view.dataset.nextChapter || '',
            verses: Array.from(document.querySelectorAll('.chapter-view__verse'))
        };
    }

    /**
     * 저장된 낭독 위치를 가져옵니다.
     *
     * @returns {object|null} 저장된 위치. 없거나 읽을 수 없으면 null
     */
    function loadState() {
        try {
            return JSON.parse(localStorage.getItem(TTS_KEY));
        } catch (e) {
            return null;
        }
    }

    /**
     * 낭독 위치를 저장합니다. 저장이 막힌 환경에서는 조용히 넘어갑니다.
     *
     * @param state 저장할 위치. null이면 기록을 지웁니다
     */
    function saveState(state) {
        try {
            if (state === null) {
                localStorage.removeItem(TTS_KEY);
            } else {
                localStorage.setItem(TTS_KEY, JSON.stringify(state));
            }
        } catch (e) {
            /* 저장하지 못해도 낭독은 계속한다 */
        }
    }

    /**
     * 버튼 글자와 색을 현재 상태에 맞춥니다.
     */
    function render() {
        const label = document.getElementById('tts-label');
        const toggle = document.getElementById('tts-toggle');
        const reset = document.getElementById('tts-reset');
        if (!label || !toggle || !reset) {
            return;
        }

        const verses = current().verses;
        if (reading) {
            label.textContent = '■ 멈춤';
        } else if (verseIndex > 0 && verseIndex < verses.length) {
            const number = verses[verseIndex].querySelector('.chapter-view__verse-num').textContent;
            label.textContent = '▶ ' + number + '절부터 이어 듣기';
        } else {
            label.textContent = '▶ 낭독 듣기';
        }

        toggle.classList.toggle('chapter-view__tts--on', reading);
        reset.hidden = verseIndex === 0;
    }

    /**
     * 지금 읽는 절에만 표시를 남기고 화면을 그 절로 옮깁니다.
     *
     * @param target 표시할 절 요소. null이면 표시를 지웁니다
     */
    function markReading(target) {
        document.querySelectorAll('.chapter-view__verse--reading')
            .forEach((verse) => verse.classList.remove('chapter-view__verse--reading'));
        if (target) {
            target.classList.add('chapter-view__verse--reading');
            target.scrollIntoView({ block: 'center', behavior: 'smooth' });
        }
    }

    /**
     * 한국어 음성으로 한 문장을 읽습니다.
     *
     * @param text    읽을 문장
     * @param onEnd   다 읽은 뒤 할 일
     * @param onStart 읽기 시작할 때 할 일
     */
    function speakText(text, onEnd, onStart) {
        const utterance = new SpeechSynthesisUtterance(text);
        speaking = utterance;
        utterance.lang = 'ko-KR';

        const korean = synth.getVoices().find((voice) => voice.lang && voice.lang.startsWith('ko'));
        if (korean) {
            utterance.voice = korean;
        }

        utterance.onstart = onStart || null;
        utterance.onend = onEnd;
        utterance.onerror = (event) => {
            if (event.error === 'interrupted' || event.error === 'canceled') {
                return;
            }
            console.warn('[tts] 음성 오류', event.error);
            stop();
        };

        synth.resume();
        synth.speak(utterance);
    }

    /**
     * 낭독을 멈추고 들은 데까지를 저장합니다. 다시 누르면 그 절부터 이어집니다.
     */
    function stop() {
        const chapterInfo = current();
        synth.cancel();
        markReading(null);
        reading = false;

        if (verseIndex > 0 && verseIndex < chapterInfo.verses.length) {
            saveState({ book: chapterInfo.book, chapter: chapterInfo.chapter, verseIndex: verseIndex });
        }

        render();
    }

    /**
     * 다음 장 내용을 받아 현재 화면에 바꿔 끼웁니다.
     * 페이지를 새로 열지 않으므로 음성이 끊기지 않습니다.
     *
     * @param book    다음 책 이름
     * @param chapter 다음 장 번호
     * @returns {Promise<boolean>} 바꿔 끼우는 데 성공하면 true
     */
    function swapChapter(book, chapter) {
        const url = '/read/' + encodeURIComponent(book) + '/' + chapter;

        return fetch(url)
            .then((response) => (response.ok ? response.text() : Promise.reject(response.status)))
            .then((html) => {
                const parsed = new DOMParser().parseFromString(html, 'text/html');
                const nextView = parsed.querySelector('.read[data-book]');
                if (!nextView) {
                    return false;
                }

                current().view.replaceWith(document.importNode(nextView, true));

                try {
                    document.title = parsed.title;
                    history.pushState({}, '', url);
                    if (typeof initChapterDetailPage === 'function') {
                        initChapterDetailPage();
                    }
                } catch (e) {
                    console.warn('[tts] 장 교체 후 처리 실패', e);
                }
                return true;
            })
            .catch((e) => {
                console.warn('[tts] 다음 장을 불러오지 못했습니다', e);
                return false;
            });
    }

    /**
     * 장을 다 읽은 뒤 다음 장을 안내하고 이어서 읽습니다.
     * 다음 장이 없으면 성경을 다 읽었다고 알리고 기록을 지웁니다.
     */
    function moveToNextChapter() {
        const chapterInfo = current();
        markReading(null);
        verseIndex = 0;

        if (!chapterInfo.nextBook || !chapterInfo.nextChapter) {
            saveState(null);
            reading = false;
            render();
            speakText('성경 읽기를 마쳤습니다.', null);
            return;
        }

        const nextBook = chapterInfo.nextBook;
        const nextChapter = Number(chapterInfo.nextChapter);

        speakText(nextBook + ' ' + nextChapter + '장으로 넘어갑니다.', () => {
            swapChapter(nextBook, nextChapter).then((swapped) => {
                if (!swapped) {
                    stop();
                    return;
                }
                saveState({ book: nextBook, chapter: nextChapter, verseIndex: 0 });
                render();

                synth.cancel();
                setTimeout(() => speak(0), 120);
            });
        });
    }

    /**
     * 해당 순서의 절을 읽고, 끝나면 다음 절로 넘어갑니다.
     *
     * @param position 읽을 절의 순서
     */
    function speak(position) {
        const verses = current().verses;

        if (position >= verses.length) {
            moveToNextChapter();
            return;
        }

        verseIndex = position;
        const verse = verses[position];

        speakText(verse.innerText, () => speak(position + 1), () => markReading(verse));
    }

    /**
     * 낭독을 시작합니다. 저장된 위치가 있으면 그 절부터 이어집니다.
     */
    function start() {
        reading = true;
        render();
        speak(verseIndex);
    }

    document.addEventListener('click', (event) => {
        if (event.target.closest('#tts-toggle')) {
            if (synth.speaking || synth.pending) {
                stop();
            } else {
                start();
            }
            return;
        }

        if (event.target.closest('#tts-reset')) {
            synth.cancel();
            markReading(null);
            reading = false;
            verseIndex = 0;
            saveState(null);
            render();
        }
    });

    const chapterInfo = current();
    const saved = loadState();
    if (saved && saved.book === chapterInfo.book && saved.chapter === chapterInfo.chapter
        && saved.verseIndex > 0 && saved.verseIndex < chapterInfo.verses.length) {
        verseIndex = saved.verseIndex;
    }
    render();

    window.addEventListener('pagehide', () => synth.cancel());
})();
