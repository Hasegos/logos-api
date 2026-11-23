/**
 * 구절 검색 화면의 스크립트.
 * 서버가 꺼져 정적 스냅샷이 뜬 경우 검색을 쓸 수 없다는 안내를 보여주고,
 * 검색 결과 본문에서 검색어와 일치하는 부분에 형광펜 표시를 입힌다.
 */
(function () {
    const root = document.querySelector('.read');
    const query = root && root.dataset.query ? root.dataset.query.trim() : '';

    showOfflineNotice(query);
    if (query) {
        highlightQuery(query);
    }

    /**
     * 서버가 응답하지 않아 정적 스냅샷이 대신 뜬 화면이면 검색을 쓸 수 없다는 안내를 보여준다.
     * 스냅샷은 주소의 검색어를 무시하고 검색어가 빈 화면을 돌려주므로,
     * 주소에는 검색어가 있는데 화면의 검색어가 비어 있으면 스냅샷으로 본다.
     *
     * @param {string} shown - 화면에 채워진 검색어
     * @returns {void}
     */
    function showOfflineNotice(shown) {
        const asked = (new URLSearchParams(window.location.search).get('q') || '').trim();
        if (!asked || shown) {
            return;
        }

        const offline = document.getElementById('search-offline');
        const tooShort = document.getElementById('search-too-short');
        if (offline) {
            offline.hidden = false;
        }
        if (tooShort) {
            tooShort.hidden = true;
        }
    }

    /**
     * 검색 결과 본문에서 검색어와 일치하는 부분을 <mark>로 감싼다.
     * 본문은 서버가 이스케이프해서 내려주므로 텍스트 노드만 쪼갠다.
     *
     * @param {string} keyword - 표시할 검색어
     * @returns {void}
     */
    function highlightQuery(keyword) {
        document.querySelectorAll('.search-hit__text').forEach(function (el) {
            const text = el.textContent;
            const fragment = document.createDocumentFragment();
            let from = 0;
            let at = text.indexOf(keyword);

            while (at !== -1) {
                fragment.appendChild(document.createTextNode(text.slice(from, at)));
                const mark = document.createElement('mark');
                mark.className = 'search-hit__mark';
                mark.textContent = text.slice(at, at + keyword.length);
                fragment.appendChild(mark);
                from = at + keyword.length;
                at = text.indexOf(keyword, from);
            }

            if (from === 0) {
                return;
            }
            fragment.appendChild(document.createTextNode(text.slice(from)));
            el.textContent = '';
            el.appendChild(fragment);
        });
    }
})();
