# 📖 성경 말씀 API (Logos API)

성경 말씀 API는 무작위로 성경 구절을 제공하며, API 키 인증과 속도 제한이 적용된 RESTful API 서비스입니다.

## ✨ 주요 기능

+ **랜덤 성경 말씀 제공**: 내부 JSON 데이터셋을 기반으로 무작위 성경 구절을 반환합니다.

+ **API 키 보안**: 발급된 API 키를 통한 인증을 수행하여 무분별한 접근을 방지합니다.

+ **스마트 캐싱**: 서버 부하 감소 및 사용자 경험 향상을 위해 **3초간의 브라우저 캐싱**을 지원합니다.

+ **속도 제한(Rate Limiting)**: 단시간 내 과도한 호출로부터 서버 리소스를 보호합니다.

## 🌐 API 주소
```http
GET https://logos-api.com/api/verse
```

**📚 전체 문서**: 인증 방법, 요청/응답 예제, 속도 제한 정책은 [logos-api.com/docs](https://logos-api.com/docs)에서 확인하세요.

## 🔑 API 키 발급 및 사용

1. **발급**: [메인 페이지](https://logos-api.com)의 새 키 발급하기 버튼을 통해 본인의 IP와 연결된 키를 생성합니다.

2. **정책**: 새 키 발급 시 기존 키는 즉시 폐기되며, 동일 IP 기준 하루 10회로 제한됩니다.

3. **인증 방법**: 모든 요청에는 HTTP 헤더에 아래 내용을 포함해야 합니다.

```
  X-API-KEY: 발급받은_UUID
```

## 📊 응답 형식

```JSON
{
  "book": "골로새서",
  "chapter": "3",
  "verse": "23",
  "text": "무엇을 하든지 마음을 다하여 주께 하듯 하고 사람에게 하듯 하지 말라",
  "reference": "골로새서 3:23"
}
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

더 많은 예제와 상세 정책은 [API 문서](https://logos-api.com/docs)를 참고하세요.

## 🤝 기여하기

새로운 성경 구절을 추가하여 프로젝트에 기여하고 싶으시면 아래 절차를 따라주세요:

1. 이 리포지토리를 Fork 합니다.

2. `src/main/resources/bible/{책}/{장}.json` 파일에 아래 형식으로 성경 말씀을 추가합니다:

```JSON
{"book":"책(서)", "chapter":"장(章)", "verse":"절(節)", "text":"본문/말씀"}
```

3. 변경 사항을 Commit한 후 Pull Request를 보냅니다.

## 🛠️ 기술 스택

+ **Backend**: Spring Boot 3.x

+ **Database**: **PostgreSQL** (API 키 정보 및 발급 로그 저장)

+ **Data Storage**: **JSON** (성경 구절 데이터셋 관리)

+ **Frontend**: Vanilla JS, CSS3, Thymeleaf

## 📈 보안 및 UX 정책

+ **API 키 인증**: 외부 요청은 발급받은 API 키로 인증합니다.

+ **속도 제한**: 서버 및 애플리케이션 단에서 과도한 요청을 제한합니다.

+ **연타 방지**: 클라이언트 단에서 버튼 클릭 후 3초간 비활성화를 강제하여 서버 부하를 줄입니다.

> 세부 구현 방식(인증 예외 조건, 임계값 등)은 악용 방지를 위해 공개하지 않습니다. 취약점을 발견하셨다면 GitHub Issue 대신 [csw020106@naver.com](mailto:csw020106@naver.com)으로 비공개 제보 부탁드립니다.

## 📄 라이선스

본 프로젝트는 **사용자 정의 라이선스 (비상업·수정 금지) v1.1**을 따릅니다.

+ **저작권자**: Su Ho Choi (@csw020106)

+ **주요 내용**: 비상업적 용도의 개인/교육/연구 목적으로만 사용 가능하며, 소스코드의 수정 및 파생 저작물 생성을 금지합니다.

+ **상업적 이용**: 별도의 서면 허가가 필요합니다 (문의: csw020106@naver.com).

상세한 내용은 [LICENSE](./LICENSE) 파일을 확인해 주세요.

© 2025 Su Ho Choi. All rights reserved

## 📚 데이터 고지

+ 본 API에서 제공하는 성경 텍스트는 **개역개정(KRV)** 판을 바탕으로 수동 입력되었습니다.

+ 성경 텍스트의 저작권은 해당 권리자(대한성서공회 등)에게 있으며, 본 프로젝트는 이를 상업적으로 이용하지 않습니다.

+ 교육 및 개인 묵상용으로만 사용해 주시기 바랍니다.

## 📬 문의

+ 이메일: csw020106@naver.com

+ 이슈: 버그/제안은 GitHub Issues로 등록해 주세요. (보안 취약점은 이슈 대신 이메일로 제보해 주세요)

+ Made with ❤️ for everyone who needs God's word