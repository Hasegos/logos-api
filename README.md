# 📖 성경 말씀 API (Logos API)

성경 말씀 API는 성경 구절을 무료로 제공하는 RESTful API 서비스입니다. 랜덤 구절 조회뿐 아니라 매일 고정되는 오늘의 말씀, 책·장·절 단위 구조적 탐색까지 지원하며, API 키 인증과 속도 제한이 적용됩니다.

## ✨ 주요 기능

+ **랜덤 성경 말씀 제공**: 내부 JSON 데이터셋을 기반으로 무작위 성경 구절을 반환합니다.
+ **오늘의 말씀** : 매일 자정 기준으로 고정된 구절을 반환합니다. 같은 날엔 누가 호출해도 같은 구절이 나옵니다.
+ **책 · 장 · 절 조회** : 원하는 책과 장을 지정해 구조적으로 탐색할 수 있습니다.
+ **웹사이트 내 성경 읽기** : [logos-api.com/read](https://logos-api.com/read)에서 구약/신약별로 책을 펼쳐 장을 고르고, 절을 클릭해 형광펜처럼 표시하거나 특정 절 링크를 공유할 수 있습니다. 이어서 읽기 위치와 읽은 장 기록은 브라우저에만 저장되며 서버로 전송되지 않습니다.
+ **API 키 보안**: 발급된 API 키를 통한 인증을 수행하여 무분별한 접근을 방지합니다.
+ **속도 제한(Rate Limiting)**: 단시간 내 과도한 호출로부터 서버 리소스를 보호합니다.

## 🌐 API 엔드포인트

| Method | 경로 | 설명 |
|---|---|---|
| GET | `/api/verse` | 무작위 구절 조회 |
| GET | `/api/verse/today` | 오늘의 말씀(날짜 기준 고정) 조회 |
| GET | `/api/books` | 데이터가 존재하는 책 목록 (정경 순서) |
| GET | `/api/books/{book}/chapters` | 해당 책의 장 번호 목록 |
| GET | `/api/books/{book}/chapters/{chapter}` | 해당 장의 전체 절 목록 |

**📚 전체 문서**: 각 엔드포인트의 상세 사용법과 응답 예시는 [logos-api.com/docs](https://logos-api.com/docs)에서 확인하세요.

## 🔑 API 키 발급 및 사용

1. **발급**: [메인 페이지](https://logos-api.com)의 API 키 발급받기 버튼을 통해 본인의 IP와 연결된 키를 생성합니다.
2. **정책**: 새 키 발급 시 기존 키는 즉시 폐기되며, 동일 IP 기준 하루 10회로 제한됩니다.
3. **인증 방법**: 모든 요청에는 아래 HTTP 헤더가 필요합니다.

   ```
   X-API-KEY: 발급받은_UUID
   ```

## 📊 응답 형식

### `/api/verse`, `/api/verse/today`

```JSON
{
  "book": "골로새서",
  "chapter": "3",
  "verse": "23",
  "text": "무엇을 하든지 마음을 다하여 주께 하듯 하고 사람에게 하듯 하지 말라",
  "reference": "골로새서 3:23"
}
```

### `/api/books`

```JSON
["창세기", "사도행전", "..."]
```

### `/api/books/{book}/chapters`

```JSON
[1, 2, 3, 4, 5]
```

### `/api/books/{book}/chapters/{chapter}`

책/장은 URL에 이미 포함되어 있으므로 절 번호와 본문만 반환합니다.

```JSON
[
  {"verse": "1", "text": "..."},
  {"verse": "2", "text": "..."}
]
```

## 📱 사용 예시

### curl

```bash
curl -H "X-API-KEY: your_api_key_here" https://logos-api.com/api/verse
```

### JavaScript

```JavaScript
const apiKey = 'your_api_key_here';

fetch('https://logos-api.com/api/verse', {
    method: 'GET',
    headers: { 'X-API-KEY': apiKey }
})
.then(res => res.json())
.then(data => console.log(data.text));
```

### Java

```Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LogosApiTest {
    public static void main(String[] args) throws Exception {
        String apiKey = "your_api_key_here";
        String url = "https://logos-api.com/api/verse";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-API-KEY", apiKey)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            System.out.println("성경 데이터: " + response.body());
        } else {
            System.out.println("호출 실패: " + response.statusCode());
        }
    }
}
```

> `/api/verse/today`, `/api/books` 계열도 호출 방식은 동일합니다 — 위 예제에서 URL만 바꾸면 됩니다.

더 많은 예제와 상세 정책은 [API 문서](https://logos-api.com/docs)를 참고하세요.

## 🤝 기여하기

새로운 성경 구절을 추가하여 프로젝트에 기여하고 싶으시면 아래 절차를 따라주세요:

1. 이 리포지토리를 Fork 합니다.
2. `src/main/resources/bible/{책}/{장}.json` 파일에 아래 형식으로 성경 말씀을 추가합니다:

```JSON
{"book":"책(서)", "chapter":"장(章)", "verse":"절(節)", "text":"본문/말씀"}
```

3. 변경 사항을 Commit한 후 Pull Request를 보냅니다.

책/장/절 번호가 숫자가 아니거나 형식이 어긋난 항목은 서버 구동 시 자동으로 검증되어 제외되니, PR 전에 형식을 한 번 확인해 주세요.

## 🛠️ 기술 스택

+ **Backend**: Spring Boot 3.x
+ **Database**: PostgreSQL (API 키 정보 및 발급 로그 저장)
+ **Data Storage**: JSON (성경 구절 데이터셋 관리)
+ **Frontend**: Vanilla JS, CSS3, Thymeleaf

## 📈 보안 및 UX 정책

+ **API 키 인증**: 외부 요청은 발급받은 API 키로 인증합니다.
+ **속도 제한**: 서버 및 애플리케이션 단에서 과도한 요청을 제한합니다.
+ **캐싱**: `/api/verse`는 3초간 브라우저 캐싱이 적용됩니다.
+ **요청 출처 검증**: 상태를 변경하는 일부 요청은 정상적인 경로로 발생했는지 검증합니다.

> 세부 구현 방식(검증 조건, 임계값 등)은 악용 방지를 위해 공개하지 않습니다. <br>
> 취약점을 발견하셨다면 GitHub Issue 대신 [csw020106@naver.com]()으로 비공개 제보 부탁드립니다.

## 📄 라이선스

본 프로젝트는 **사용자 정의 라이선스 (비상업·수정 금지) v1.1**을 따릅니다.

+ **저작권자**: Su Ho Son (@csw020106)
+ **주요 내용**: 비상업적 용도의 개인/교육/연구 목적으로만 사용 가능하며, 소스코드의 수정 및 파생 저작물 생성을 금지합니다.
+ **상업적 이용**: 별도의 서면 허가가 필요합니다 (문의: csw020106@naver.com).

상세한 내용은 [LICENSE](./LICENSE) 파일을 확인해 주세요.

© 2025 Su Ho Son. All rights reserved

## 📚 데이터 고지

+ 본 API에서 제공하는 성경 텍스트는 **개역개정(KRV)** 판을 바탕으로 수동 입력되었습니다.
+ 성경 텍스트의 저작권은 해당 권리자(대한성서공회 등)에게 있으며, 본 프로젝트는 이를 상업적으로 이용하지 않습니다.
+ 교육 및 개인 묵상용으로만 사용해 주시기 바랍니다.

## 📬 문의

+ 이메일: csw020106@naver.com
+ 이슈: 버그/제안은 GitHub Issues로 등록해 주세요. (보안 취약점은 이슈 대신 이메일로 제보해 주세요)
+ Made with ❤️ for everyone who needs God's word