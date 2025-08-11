# 🔔 알림 시스템 테스트 가이드

## 📋 개요
이 가이드는 HowAreYou 프로젝트의 알림 시스템을 테스트하는 방법을 설명합니다.

## 🚀 테스트 방법

### 1. 정적 HTML 테스트 (간단한 API 테스트)
- URL: `http://localhost:8080/notification-test.html`
- 브라우저에서 직접 접근 가능
- SSE 연결 및 알림 발송 테스트

### 2. React 프론트엔드 테스트 (실제 프론트엔드 연동)
- URL: `http://localhost:3000/notification-test`
- React 앱에서 테스트
- TypeScript 기반 컴포넌트

## 🛠️ 테스트 준비

### 백엔드 서버 실행
```bash
# 프로젝트 루트 디렉토리에서
./gradlew bootRun
```

### React 프론트엔드 실행
```bash
# /Users/taco/social-login-frontend 디렉토리에서
npm run dev
```

### 데이터베이스 및 Redis 실행
```bash
# 프로젝트 루트 디렉토리에서
docker-compose up -d
```

## 📡 SSE 연결 테스트

### 1. 연결 설정
- 사용자명 입력 (예: `testuser1`)
- "연결" 버튼 클릭
- 연결 상태 확인

### 2. 하트비트 확인
- 25초마다 ping 이벤트 수신
- 자동 하트비트 응답 전송
- Redis TTL 갱신으로 온라인 상태 유지
- 연결 유지 상태 확인

## 📤 알림 발송 테스트

### 채팅 알림 테스트
```json
{
  "receiverName": "testuser1",
  "roomId": 1,
  "senderId": 2,
  "preview": "안녕하세요! 새로운 메시지가 도착했습니다."
}
```

### 시스템 알림 테스트
```json
{
  "receiverName": "testuser1",
  "message": "시스템 알림 테스트 메시지입니다."
}
```

## 🔧 API 엔드포인트

### SSE 연결
- `GET /api/v1/notifications/sse/{memberName}`

### 하트비트
- `POST /api/v1/notifications/heartbeat/{memberName}` - 하트비트 응답

### 테스트 API
- `POST /api/test/notifications/send-chat` - 채팅 알림 발송
- `POST /api/test/notifications/send-system` - 시스템 알림 발송
- `GET /api/test/notifications/status/{memberName}` - 연결 상태 확인
- `GET /api/test/notifications/test-users` - 테스트 사용자 목록

## 📊 테스트 시나리오

### 시나리오 1: 기본 연결 테스트
1. 사용자명으로 SSE 연결
2. 연결 상태 확인
3. ping 이벤트 수신 확인
4. 자동 하트비트 응답 확인
5. Redis TTL 갱신 확인

### 시나리오 2: 실시간 알림 테스트
1. 사용자 A로 SSE 연결
2. 사용자 B에서 사용자 A에게 알림 발송
3. 실시간 수신 확인

### 시나리오 3: 오프라인 알림 테스트
1. 사용자 A 연결 해제
2. 사용자 A에게 알림 발송
3. 사용자 A 재연결
4. 미전송 알림 자동 수신 확인

### 시나리오 4: 다중 사용자 테스트
1. 여러 브라우저에서 다른 사용자로 연결
2. 사용자 간 알림 발송
3. 각 사용자별 수신 확인

## 🐛 문제 해결

### CORS 오류
- 백엔드 CORS 설정 확인
- `application.yml`의 `front.cors.allowed-origins` 설정 확인

### 연결 실패
- Redis 서버 실행 상태 확인
- 포트 8080 사용 가능 여부 확인

### 알림 수신 안됨
- SSE 연결 상태 확인
- 브라우저 개발자 도구에서 네트워크 탭 확인
- 서버 로그 확인

## 📝 로그 확인

### 백엔드 로그
```bash
# 애플리케이션 로그 확인
tail -f logs/application.log
```

### 브라우저 개발자 도구
- Network 탭에서 SSE 연결 상태 확인
- Console 탭에서 JavaScript 오류 확인

## 🔄 알림 시스템 아키텍처

```
클라이언트 ←→ SSE ←→ RedisEmitter ←→ NotificationPushService ←→ NotificationRepository
    ↓           ↓
하트비트    SsePingScheduler (25초마다 ping)
응답
```

## 📚 추가 정보

- **SSE 타임아웃**: 6시간
- **Ping 주기**: 25초
- **Redis TTL**: 2분 (온라인 상태)
- **알림 타입**: CHAT, SYSTEM, FOLLOW

## 🎯 성공 기준

✅ SSE 연결 성공  
✅ 실시간 알림 수신  
✅ 오프라인 알림 저장 및 재전송  
✅ 하트비트 정상 동작 (ping 수신 → 응답 전송)  
✅ Redis TTL 자동 갱신  
✅ 다중 사용자 지원  
✅ CORS 정상 동작 