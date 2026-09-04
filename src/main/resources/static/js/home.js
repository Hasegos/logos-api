/**
 * 홈페이지의 API 키 발급 버튼과 복사 버튼에 각각 이벤트 리스너를 연결합니다.
 */
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('generate-key-btn').addEventListener('click', generateKey);
    document.getElementById('copy-key-btn').addEventListener('click', copyToClipboard);
});

/**
 * 서버에 새 API 키 발급을 요청하고, 성공 시 발급된 키를 화면에 표시합니다.
 * 발급 실패(예: 하루 발급 제한 초과) 시에는 알림창으로 오류 메시지를 보여줍니다.
 *
 * @returns {void}
 */
function generateKey() {
    fetch('/api/key/generate', { method: 'POST' })
        .then(response => response.text())
        .then(key => {
            const container = document.getElementById('key-container');
            const keyElement = document.getElementById('api-key');
            keyElement.innerText = key;
            container.style.display = 'block';
            document.getElementById('copy-msg').style.display = 'none';
        })
        .catch(err => alert('발급 실패: ' + err));
}

/**
 * 현재 화면에 표시된 API 키를 클립보드에 복사하고, 복사 완료 메시지를 표시합니다.
 *
 * @returns {void}
 */
function copyToClipboard() {
    const keyText = document.getElementById('api-key').innerText;
    navigator.clipboard.writeText(keyText).then(() => {
        document.getElementById('copy-msg').style.display = 'block';
    });
}