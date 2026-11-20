/**
 * 문서 페이지의 코드 블록에 언어별 색을 입힙니다.
 * 색만 담당하므로 하이라이터가 없거나 실패해도 코드는 그대로 읽힙니다.
 */
document.addEventListener('DOMContentLoaded', () => {
    if (typeof hljs === 'undefined') {
        return;
    }

    hljs.configure({ ignoreUnescapedHTML: true });
    document.querySelectorAll('pre code[class*="language-"]').forEach((block) => {
        hljs.highlightElement(block);
    });
});
