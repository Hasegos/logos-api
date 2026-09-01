document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('generate-key-btn').addEventListener('click', generateKey);
    document.getElementById('copy-key-btn').addEventListener('click', copyToClipboard);
});

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

function copyToClipboard() {
    const keyText = document.getElementById('api-key').innerText;
    navigator.clipboard.writeText(keyText).then(() => {
        document.getElementById('copy-msg').style.display = 'block';
    });
}