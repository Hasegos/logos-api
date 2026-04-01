# 📖 성경 말씀 API (Logos API)

성경 말씀 API는 무작위로 성경 구절을 제공하며, API 키 인증과 속도 제한이 적용된 RESTful API 서비스입니다.

## ✨ 주요 기능

+ **랜덤 성경 말씀 제공**: 내부 `verses.json` 데이터를 기반으로 무작위 성경 구절을 반환합니다.

+ **API 키 보안**: 발급된 API 키를 통한 인증을 수행하여 무분별한 접근을 방지합니다.

+ **스마트 캐싱**: 서버 부하 감소 및 사용자 경험 향상을 위해 **3초간의 브라우저 캐싱**을 지원합니다.

+ **속도 제한(Rate Limiting)**: 단시간 내 과도한 호출로부터 서버 리소스를 보호합니다.

## 🌐 API 주소
```http
GET https://logos-api.com/api/verse
```

**보안 안내**: 본 서비스는 CORS 정책 및 Referer 체크를 적용하고 있어, 허용되지 않은 외부 도메인의 브라우저 콘솔 호출은 차단됩니다.

## 🔑 API 키 발급 및 사용

1. **발급**: 메인 페이지의 새 키 발급하기 버튼을 통해 본인의 IP와 연결된 키를 생성합니다.

2. **정책**: 새 키 발급 시 기존 키는 즉시 폐기되며, 보안을 위해 PostgreSQL DB에 발급 로그가 기록됩니다.

3. **인증 방법**: 외부 호출 시 HTTP 요청 헤더에 아래 내용을 포함해야 합니다.

   + `Header Key: X-API-KEY`

   + `Header Value: 발급받은_UUID`

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
## 📱 사용 예시 (JavaScript)

우리 페이지(`logos-api.com`) 내에서는 Referer 체크를 통해 키 없이 호출 가능하지만, 외부 인터페이스에서는 다음과 같이 호출해야 합니다.

```javascript
const apiKey = 'your_api_key_here';

fetch('https://logos-api.com/api/verse', {
    method: 'GET',
    headers: { 'X-API-KEY': apiKey }
})
.then(res => res.json())
.then(data => console.log(data.text));
```

## 🛠️ 기술 스택

+ **Backend**: Spring Boot 3.x

+ **Database**: **PostgreSQL** (API 키 정보 및 발급 로그 저장)

+ **Data Storage**: **JSON** (성경 구절 데이터셋 관리)

+ **Frontend**: Vanilla JS, CSS3, Thymeleaf (메인 단일 페이지 구성)

## 📈 보안 및 UX 정책

+ **Referer 검증**: 서비스 메인 페이지를 통한 접근은 API 키 검사를 면제하여 편의성을 높였습니다.

+ **연타 방지**: 클라이언트 단에서 버튼 클릭 후 **3초간 비활성화**를 강제하여 서버 부하를 줄이고 묵상을 돕습니다.

+ **CORS 보호**: 허용된 도메인 외의 브라우저 환경 접근을 원천 차단합니다.

## 📄 라이선스

+ © 2025-2026 Su Ho Choi. All rights reserved.

+ 비상업적 용도로만 사용 가능하며, 데이터 무단 전재 및 재배포를 금합니다.

## 📬 문의

+ 이메일: csw020106@naver.com

+ 이슈: 버그/제안은 GitHub Issues로 등록해 주세요.

+ Made with ❤️ for everyone who needs God's word