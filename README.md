# KTMatch - 한기대 학생 전용 게시판 기반 그룹 매칭(과팅) 서비스

> **Spring Boot 3.x/4.x와 Java 21 가상 스레드를 활용한 고성능 비동기 매칭 및 채팅 백엔드 시스템**

본 프로젝트는 불특정 다수와의 랜덤 매칭이 아닌, **'검증된 한기대 학생 그룹 간의 선택형 매칭'**을 제공하는 과팅(Group Blind Date) 앱의 핵심 API 서버입니다. RESTful API 기반의 비동기 쪽지 시스템과 FCM 푸시 알림, 그리고 매칭 성사 시 제공되는 STOMP 기반 실시간 그룹 채팅 기능을 포함하고 있습니다.

---

## 🛠 Tech Stack
- **Backend:** Java 21 (Virtual Threads), Spring Boot
- **Database:** MySQL 8.0 (Data Persistence), Redis (Caching & Session)
- **Security:** Spring Security, JWT (JSON Web Token), UnivCert (대학 이메일 인증)
- **Real-time & Async:** Spring WebSocket (STOMP), Firebase Cloud Messaging (FCM)
- **Infrastructure:** NCP (Naver Cloud Platform) - *예정*, Docker

---

## 📌 핵심 구현 로직 및 기능 (Core Features)

### 1. 🎓 철저한 대학생 인증 및 유저 관리
- **대학 이메일 인증:** `UnivCert` API 등을 연동하여 실제 대학생(학과, 학번) 임을 검증한 유저만 가입 및 활동 가능.
- **JWT 기반 무상태 인증:** 모바일 앱 클라이언트와의 통신을 위해 Header 기반의 JWT 인증(Access/Refresh Token) 도입.
- **그룹 프로필 확장:** 기존 단일 유저 정보뿐만 아니라 "남자 3명, 컴퓨터공학과 26학번" 등 그룹 단위의 메타데이터를 관리하는 설계.

### 2. 📝 게시판(Board) 기반 매칭 시스템
- **과팅 포스팅 (Post):** 방장이 과팅을 희망하는 인원수, 학과, 키워드 등을 등록하여 게시글(Post)을 작성.
- **필터링 및 조회:** 동시 접속 매칭(Queue) 대신, 유저가 원하는 조건의 게시글을 탐색하고 조회하는 REST API 환경 구축.
- **상태 관리:** 모집 중(RECRUITING), 마감(CLOSED) 등 게시글의 생명주기 관리.

### 3. ✉️ 비동기 쪽지(Note) 및 알림(Push) 시스템
- **쪽지 전송 및 재화 소모:** 마음에 드는 게시글에 쪽지(호감)를 보낼 때 '티켓(재화)'이 소모되며, Spring `@Transactional`을 통해 결제 내역과 쪽지 발송의 무결성 보장.
- **읽음/안 읽음(Read Status) 처리:** 비동기 환경에 맞춰 수신자가 쪽지를 열람했는지 상태(isRead)를 추적.
- **FCM 푸시 알림:** 상대방이 앱을 종료한 상태라도 Firebase Admin SDK를 통해 "새로운 쪽지가 도착했습니다" 혹은 "쪽지가 수락되었습니다" 등의 실시간 푸시 알림 발송.

### 4. 💬 STOMP 기반 실시간 단톡방 생성
- **쌍방 수락 시 방 개설:** 쪽지를 받은 그룹의 방장이 '수락(Accept)'을 누르면, 양쪽 그룹원 전원이 참여하는 `ChatRoom` 및 `ChatParticipant` 데이터가 자동 생성.
- **이중 보안 소켓 연결:** Handshake 단계 및 STOMP CONNECT 헤더에서 JWT를 검증하여 허가된 유저만 해당 단톡방에 접속하도록 보안 강화.
- **영구적 대화 보존:** 1회성 랜덤 채팅과 달리, DB에 대화 내역(`ChatMessage`)을 영속화하여 카카오톡처럼 언제든 이전 대화를 조회 가능.

---

## 🏗 시스템 워크플로우 (Matching Workflow)

1. **포스팅 등록 (Post):** A 그룹(방장)이 "남자 3명, 컴공과" 게시글 등록.
2. **탐색 및 쪽지 전송 (Send Note):** B 그룹이 게시판에서 A 그룹을 발견하고 API(`POST /api/notes`)를 통해 쪽지 발송 (재화 차감).
3. **푸시 알림 (FCM):** 서버가 A 그룹 방장의 디바이스 토큰을 조회하여 FCM 푸시 알림 발송.
4. **확인 및 수락 (Accept):** A 그룹이 쪽지를 읽고(상태 변경) 수락 API 호출.
5. **채팅방 개설 (Chat Room):** 서버에서 A, B 그룹 6명을 묶는 WebSocket 채팅방 생성.
6. **실시간 통신 (STOMP):** 이후 앱 내에서 실시간 그룹 채팅 진행.

---

## 🚀 향후 발전 계획 (Future Scope)
- **Redis 캐싱 도입:** 메인 화면의 인기 게시글이나 잦은 조회가 발생하는 학과/대학 목록을 Redis에 캐싱하여 조회 성능 최적화.
- **CI/CD 파이프라인 구축:** GitHub Actions와 Docker를 활용하여 NCP 서버에 자동 배포 환경 구축.
- **신고 및 고도화:** 채팅방 내 불건전 유저 신고 기능.

---

## ✅ 현재 구현 상태 (2026-04-17)
- 인증/유저 도메인 우선 구현 완료:
  - 한기대 이메일 인증 코드 발급 API
  - 이메일 인증 코드 검증 API
  - 인증 완료 후 회원가입 API
  - 로그인 API (JWT Access Token 발급)
  - 내 정보 조회 API (`GET /api/v1/users/me`)
- 이메일 인증은 실제 메일 서버 연동 없이 **콘솔 로그 출력 방식**으로 동작합니다.
- 성인 인증 로직은 현재 스코프에서 제외되어 있으며, 테스트 시 수동 검증 전제로 진행합니다.

## 📮 인증 API 요약
- `POST /api/v1/auth/email/send`
  - body: `{ "email": "xxxx@koreatech.ac.kr" }`
  - 동작: 6자리 인증 코드 생성 후 DB 저장 + 콘솔 로그 출력
- `POST /api/v1/auth/email/verify`
  - body: `{ "email": "xxxx@koreatech.ac.kr", "code": "123456" }`
  - 동작: 코드 일치 및 만료 확인 후 인증 완료 처리
- `POST /api/v1/auth/signup`
  - body: `{ "email": "...", "password": "...", "name": "...", "studentNumber": "...", "major": "COMPUTER_SCIENCE" }`
  - 동작: 이메일 인증 확인 후 회원가입 + JWT 발급
- `POST /api/v1/auth/login`
  - body: `{ "email": "...", "password": "..." }`
  - 동작: 로그인 성공 시 JWT 발급

## ▶️ 실행
```bash
./gradlew bootRun
```
