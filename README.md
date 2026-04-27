# KTMatch Backend

한기대 학생 대상 과팅 서비스 백엔드입니다.

## 현재 구현 범위
- 한기대 이메일(`@koreatech.ac.kr`) 인증 코드 발급/검증
- 회원가입
- 학생증 이미지 업로드
- 관리자 수동 승인/반려 기반 학생증 인증
- 로그인(JWT)
  - 로그인 자체는 가능
  - 단, 보호 기능 API 접근 시 이메일/학생증 인증 상태를 검사
- 내 정보 조회/수정/삭제
- 포스트 생성/목록/수정/삭제
  - 멤버별 `학과 + 학번` 입력
  - 멤버 학번이 `users` DB에 존재해야 함
  - 학번-학과 조합이 `users` 정보와 일치해야 함
- 채팅(쪽지) 생성/목록/메시지 전송/메시지 조회
  - 포스트 기반 1:1 채팅방 생성
  - 본인 포스트에는 채팅방 생성 불가
  - 채팅방 참여자만 메시지 조회/전송 가능

## 현재 정책
- 성인 인증: 미구현(요청 전까지 미적용)
- 이메일 발송: SMTP 미연동, 서버 콘솔 로그 출력
- 학생증 인증: 자동 판독 없음, 관리자 수동 승인
- 인증 토큰: `Authorization Bearer` 헤더 + `HttpOnly 쿠키(ACCESS_TOKEN)` 둘 다 지원

## 실행
```bash
./gradlew bootRun
```

## 로컬 페이지
- 메인(사용법): `http://localhost:8080/`
- 매칭보드: `http://localhost:8080/feed.html`
- 회원가입: `http://localhost:8080/signup.html`
- 계정 센터: `http://localhost:8080/account.html`
- 쪽지함(방 목록): `http://localhost:8080/chat.html`
- 채팅방(단일): `http://localhost:8080/chat-room.html?roomId={id}`
- 관리자 학생증 승인 UI: `http://localhost:8080/admin-student-card.html`
- DB Viewer: `http://localhost:8080/db-viewer.html`
- H2 Console: `http://localhost:8080/h2-console`

H2 기본 접속:
- JDBC URL: `jdbc:h2:mem:koreatech-major-meeting`
- user: `sa`
- password: 빈값

## 인증/회원 API

### 1) 이메일 코드 발급
- `POST /api/v1/auth/email/send`
```json
{
  "email": "tester@koreatech.ac.kr"
}
```

### 2) 이메일 코드 검증
- `POST /api/v1/auth/email/verify`
```json
{
  "email": "tester@koreatech.ac.kr",
  "code": "123456"
}
```

### 3) 회원가입
- `POST /api/v1/auth/signup`
```json
{
  "email": "tester@koreatech.ac.kr",
  "password": "password1234",
  "name": "Tester",
  "studentNumber": "20261234",
  "major": "COMPUTER_SCIENCE"
}
```

### 4) 로그인
- `POST /api/v1/auth/login`
```json
{
  "email": "tester@koreatech.ac.kr",
  "password": "password1234"
}
```
- 로그인/회원가입 성공 시 `HttpOnly ACCESS_TOKEN` 쿠키도 함께 발급됩니다.

보호 기능 API 접근 시 인증 상태 에러:
- 이메일 미인증: `AUTH_401_2`
- 학생증 미승인: `AUTH_401_3`

### 5) 로그아웃
- `POST /api/v1/auth/logout`
- `HttpOnly ACCESS_TOKEN` 쿠키 삭제

### 6) 내 정보 조회
- `GET /api/v1/users/me`
- Header: `Authorization: Bearer {accessToken}`

### 7) 내 정보 수정
- `PUT /api/v1/users/me`
- Header: `Authorization: Bearer {accessToken}`
```json
{
  "name": "TesterUpdated",
  "major": "DESIGN"
}
```

### 8) 회원 탈퇴
- `DELETE /api/v1/users/me`
- Header: `Authorization: Bearer {accessToken}`

## 학생증 인증 API

### 1) 학생증 업로드
- `POST /api/v1/users/me/student-card`
- Header: `Authorization: Bearer {accessToken}`
- Content-Type: `multipart/form-data`
- Form field: `file` (image/*)

### 2) 내 학생증 상태 조회
- `GET /api/v1/users/me/student-card/status`
- Header: `Authorization: Bearer {accessToken}`

### 3) 관리자 대기목록 조회
- `GET /api/v1/admin/student-cards/pending`
- Header: `Authorization: Bearer {adminAccessToken}`

### 4) 관리자 이미지 조회
- `GET /api/v1/admin/student-cards/{requestId}/image`
- Header: `Authorization: Bearer {adminAccessToken}`

### 5) 관리자 승인
- `POST /api/v1/admin/student-cards/{requestId}/approve`
- Header: `Authorization: Bearer {adminAccessToken}`

### 6) 관리자 반려
- `POST /api/v1/admin/student-cards/{requestId}/reject`
- Header: `Authorization: Bearer {adminAccessToken}`
```json
{
  "reason": "학생증 정보 확인 불가"
}
```

### 7) 개발용 관리자 승격
- `POST /api/v1/dev/db/admin/promote?email={koreatechEmail}`
- 로컬 개발/테스트 용도

## 포스트 API

### 1) 포스트 생성
- `POST /api/v1/posts`
- Header: `Authorization: Bearer {accessToken}`
```json
{
  "totalMemberCount": 2,
  "introduction": "멤버별 과/학번 입력 포스트",
  "memberProfiles": [
    { "major": "COMPUTER_SCIENCE", "studentNumber": "20241234" },
    { "major": "MECHANICAL", "studentNumber": "20241235" }
  ]
}
```

### 2) 포스트 목록
- `GET /api/v1/posts?page=0&size=20`
- Header: `Authorization: Bearer {accessToken}`

### 3) 포스트 수정
- `PUT /api/v1/posts/{postId}`
- Header: `Authorization: Bearer {accessToken}`

### 4) 포스트 삭제
- `DELETE /api/v1/posts/{postId}`
- Header: `Authorization: Bearer {accessToken}`

## 채팅 API

### 1) 채팅방 생성 (포스트 기반)
- `POST /api/v1/chats/rooms`
- Header: `Authorization: Bearer {accessToken}`
```json
{
  "postId": 1
}
```

### 2) 내 채팅방 목록
- `GET /api/v1/chats/rooms?page=0&size=20`
- Header: `Authorization: Bearer {accessToken}`

### 3) 메시지 전송
- `POST /api/v1/chats/rooms/{roomId}/messages`
- Header: `Authorization: Bearer {accessToken}`
```json
{
  "content": "안녕하세요! 포스트 보고 연락드려요."
}
```

### 4) 메시지 조회
- `GET /api/v1/chats/rooms/{roomId}/messages?size=30&beforeMessageId=100`
- Header: `Authorization: Bearer {accessToken}`

## 공통 응답 포맷
```json
{
  "success": true,
  "data": {},
  "error": null
}
```

실패 예시:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "POST_400_2",
    "message": "포스트 멤버 학번이 가입된 사용자 DB에 존재하지 않습니다."
  }
}
```

## 테스트
```bash
./gradlew test
```
