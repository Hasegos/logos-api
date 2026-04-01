function generateKey() {
    fetch('/api/key/generate', { method: 'POST' })
        .then(response => response.text())
        .then(key => {
            currentKey = key;
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

async function getVerse() {

    const btn = event.currentTarget;
    const textElem = document.getElementById('verse-text');
    const container = document.getElementById('verse-container');
    const refElem = document.getElementById('verse-ref');

    btn.disabled = true;
    btn.innerText = "말씀을 묵상하는 중...";
    btn.style.backgroundColor = "#999";
    btn.style.cursor = "not-allowed";

    container.style.display = 'block';
    textElem.style.opacity = "0.5";
    textElem.innerText = "잠시만 기다려주세요...";
    refElem.innerText = "";

    try {
        const response = await fetch('/api/verse', {
            method: 'GET',
        });

        if (!response.ok) throw new Error('데이터를 가져오지 못했습니다.');
        const data = await response.json();

        textElem.innerText = `"${data.text}"`;
        refElem.innerText = `- ${data.reference}`;
        textElem.style.opacity = "1";

    } catch (err) {
        alert(err.message);
        textElem.innerText = "오류가 발생했습니다. 다시 시도해주세요.";
    } finally {
        setTimeout(() => {
            btn.disabled = false;
            btn.innerText = "오늘의 말씀 보기";
            btn.style.backgroundColor = "#673AB7";
            btn.style.cursor = "pointer";
        }, 3000);
    }
}