/** 복사 버튼이 만드는 엔드포인트 주소의 앞부분 */
const LOGOS_API_BASE_URL = 'https://logos-api.com';

/**
 * 페이지 내 모든 코드 블록(pre)과 엔드포인트 카드에 복사 버튼을 자동으로 부착합니다.
 * 이미 버튼이 붙어있는 요소는 중복 처리하지 않습니다.
 */
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('pre').forEach((pre) => {
        if (pre.querySelector('.code-block__copy-btn')) return;

        pre.classList.add('code-block');

        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'code-block__copy-btn';
        button.textContent = '복사';
        button.setAttribute('aria-label', '코드 복사');

        /**
        * 복사 버튼 클릭 시, 해당 코드 블록의 텍스트를 클립보드에 복사하고
        * 성공/실패 여부에 따라 버튼에 피드백 메시지를 표시합니다.
        */
        button.addEventListener('click', () => {
            const codeEl = pre.querySelector('code');
            const text = codeEl ? codeEl.innerText : pre.innerText;

            navigator.clipboard.writeText(text)
                .then(() => showFeedback(button, '복사됨!'))
                .catch(() => showFeedback(button, '복사 실패'));
        });

        pre.appendChild(button);
    });

    document.querySelectorAll('.endpoint').forEach((endpoint) => {
        if (endpoint.querySelector('.code-block__copy-btn')) return;

        const pathEl = endpoint.querySelector('.endpoint__path');
        if (!pathEl) return;

        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'code-block__copy-btn';
        button.textContent = '복사';
        button.setAttribute('aria-label', '요청 주소 복사');

        /**
         * 복사 버튼 클릭 시 운영 주소를 앞에 붙인 전체 URL을 클립보드에 복사합니다.
         * 경로만 복사하면 호출에 바로 쓸 수 없어 주소를 합쳐서 넣습니다.
         */
        button.addEventListener('click', () => {
            const url = LOGOS_API_BASE_URL + pathEl.innerText.trim();

            navigator.clipboard.writeText(url)
                .then(() => showFeedback(button, '복사됨!'))
                .catch(() => showFeedback(button, '복사 실패'));
        });

        endpoint.appendChild(button);
    });
});

/**
 * 복사 버튼에 임시 피드백 메시지를 표시한 뒤, 일정 시간이 지나면 원래 상태로 되돌립니다.
 * 피드백이 표시되는 동안에는 버튼을 비활성화하여 중복 클릭을 방지합니다.
 *
 * @param {HTMLButtonElement} button - 피드백을 표시할 복사 버튼 요소
 * @param {string} message - 버튼에 임시로 표시할 메시지 (예: "복사됨!", "복사 실패")
 * @returns {void}
 */
function showFeedback(button, message) {
    const original = button.textContent;
    button.textContent = message;
    button.disabled = true;
    setTimeout(() => {
        button.textContent = original;
        button.disabled = false;
    }, 1500);
}