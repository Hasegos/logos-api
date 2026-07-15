document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('pre').forEach((pre) => {
        if (pre.querySelector('.copy-code-btn')) return;

        pre.classList.add('code-block');

        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'copy-code-btn';
        button.textContent = '복사';
        button.setAttribute('aria-label', '코드 복사');

        button.addEventListener('click', () => {
            const codeEl = pre.querySelector('code');
            const text = codeEl ? codeEl.innerText : pre.innerText;

            navigator.clipboard.writeText(text)
                .then(() => showFeedback(button, '복사됨!'))
                .catch(() => showFeedback(button, '복사 실패'));
        });

        pre.appendChild(button);
    });
});

function showFeedback(button, message) {
    const original = button.textContent;
    button.textContent = message;
    button.disabled = true;
    button.classList.add('copy-code-btn--active');
    setTimeout(() => {
        button.textContent = original;
        button.disabled = false;
        button.classList.remove('copy-code-btn--active');
    }, 1500);
}