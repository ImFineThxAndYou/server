# 🔔 알림 시스템 요약

## 📋 구현 개요
- **기술 스택**: Spring Boot + SSE + Redis + PostgreSQL
- **주요 기능**: 실시간 알림, 오프라인 알림 저장, 하트비트 연결 유지
- **지원 알림 타입**: 채팅, 채팅 요청, 시스템 알림

## 🏗️ 핵심 컴포넌트

| 컴포넌트 | 역할 | 주요 기능 |
|---------|------|-----------|
| `Notification` | 엔티티 | 알림 데이터 모델, 상태 관리 |
| `NotificationPushService` | 서비스 | 알림 발송 로직, 온/오프라인 처리 |
| `RedisEmitter` | SSE 관리 | 연결 관리, 온라인 상태 추적 |
| `NotificationController` | 컨트롤러 | SSE 연결, 하트비트 처리 |
| `SsePingScheduler` | 스케줄러 | 15초마다 ping 전송 |

## 🔄 주요 플로우

### 1. 실시간 알림
```
알림 발생 → DB 저장 → SSE 연결 확인 → 실시간 전송
```

### 2. 오프라인 알림
```
알림 발생 → DB 저장 → 재연결 시 미전송 알림 전송
```

### 3. 연결 유지
```
SSE 연결 → 15초마다 ping → 25초마다 하트비트 → Redis TTL 갱신
```

## 📊 데이터 구조

### Notification 테이블
- `id`: UUID (PK)
- `receiver_id`: 수신자 ID
- `type`: 알림 타입 (CHAT, CHATREQ, SYSTEM)
- `payload`: JSONB (알림 내용)
- `created_at`: 생성 시간
- `delivered_at`: 전송 시간
- `read_at`: 읽음 시간

### Redis 키
- `sse:online:{memberId}`: 온라인 상태 (TTL: 2분)

## 🛠️ API 엔드포인트

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/api/v1/notify/sse` | SSE 연결 구독 |
| POST | `/api/v1/notify/heartbeat` | 하트비트 응답 |
| POST | `/api/test/notifications/send-chat` | 채팅 알림 테스트 |
| POST | `/api/test/notifications/send-system` | 시스템 알림 테스트 |

## ⚙️ 설정 값

| 항목 | 값 | 설명 |
|------|-----|------|
| SSE 타임아웃 | 6시간 | 연결 유지 시간 |
| Ping 주기 | 15초 | 서버에서 ping 전송 |
| 하트비트 주기 | 25초 | 클라이언트 응답 |
| Redis TTL | 2분 | 온라인 상태 유지 |

## 🧪 테스트 방법

1. **정적 HTML**: `http://localhost:8080/notification-test.html`
2. **API 테스트**: curl 명령어로 직접 테스트
3. **연결 상태 확인**: `/api/test/notifications/status/{memberName}`

## 🚀 성능 특징

- **실시간 전송**: 온라인 사용자에게 즉시 전송
- **오프라인 저장**: 오프라인 사용자 알림 자동 저장
- **연결 유지**: 하트비트로 안정적인 연결 관리
- **확장 가능**: Redis 기반 다중 인스턴스 지원

## 🔮 향후 개선

- 알림 읽음 처리 API
- 알림 히스토리 조회
- 푸시 알림 (FCM) 연동
- 알림 설정 관리 