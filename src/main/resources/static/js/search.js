/**
 * 검색 결과 본문에서 검색어와 일치하는 부분에 형광펜 표시를 입힌다.
 * 본문은 서버가 이스케이프해서 내려주므로, 여기서는 텍스트 노드만 쪼개 <mark>로 감싼다.
 */
(function () {
    const root = document.querySelector('.read');
    const query = root && root.dataset.query ? root.dataset.query.trim() : '';
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
