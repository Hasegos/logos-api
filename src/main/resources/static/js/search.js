/**
 * 구절 검색 화면의 스크립트.
 * 1) 서버가 꺼져 정적 스냅샷이 대신 뜬 경우에 검색을 쓸 수 없다는 안내를 보여준다.
 * 2) 검색 결과 본문에서 검색어와 일치하는 부분에 형광펜 표시를 입힌다.
 *    본문은 서버가 이스케이프해서 내려주므로, 여기서는 텍스트 노드만 쪼개 <mark>로 감싼다.
 */
(function () {
    const root = document.querySelector('.read');
    const query = root && root.dataset.query ? root.dataset.query.trim() : '';

    // 스냅샷은 정적 파일이라 ?q= 를 무시하고 검색어가 빈 화면을 그대로 돌려준다.
    // 주소에는 검색어가 있는데 화면의 검색어가 비어 있으면 서버가 응답하지 않은 것이다.
    const asked = (new URLSearchParams(window.location.search).get('q') || '').trim();
    if (asked && !query) {
        const offline = document.getElementById('search-offline');
        const tooShort = document.getElementById('search-too-short');
        if (offline) {
            offline.hidden = false;
        }
        if (tooShort) {
            tooShort.hidden = true;
        }
    }

    if (!query) {
        return;
    }

    document.querySelectorAll('.search-hit__text').forEach(function (el) {
        const text = el.textContent;
        const fragment = document.createDocumentFragment();
        let from = 0;
        let at = text.indexOf(query);

        while (at !== -1) {
            fragment.appendChild(document.createTextNode(text.slice(from, at)));
            const mark = document.createElement('mark');
            mark.className = 'search-hit__mark';
            mark.textContent = text.slice(at, at + query.length);
            fragment.appendChild(mark);
            from = at + query.length;
            at = text.indexOf(query, from);
        }

        if (from === 0) {
            return;
        }
        fragment.appendChild(document.createTextNode(text.slice(from)));
        el.textContent = '';
        el.appendChild(fragment);
    });
})();
