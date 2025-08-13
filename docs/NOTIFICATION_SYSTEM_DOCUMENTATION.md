# 🔔 알림 시스템 구현 문서

## 📋 개요
HowAreYou 프로젝트의 실시간 알림 시스템은 Server-Sent Events (SSE)를 기반으로 구현되었으며, Redis를 활용한 온라인 상태 관리와 함께 실시간 알림 전송 및 오프라인 알림 저장 기능을 제공합니다.

## 🏗️ 시스템 아키텍처

```
┌─────────────┐    SSE    ┌─────────────┐    Redis    ┌─────────────┐
│   Client    │ ←──────→ │  Backend    │ ←──────→ │   Redis     │
│ (Browser)   │          │ (Spring)    │          │ (Cache)     │
└─────────────┘          └─────────────┘          └─────────────┘
                                │
                                ▼
                        ┌─────────────┐
                        │ PostgreSQL  │
                        │ (Database)  │
                        └─────────────┘
```

## 📦 핵심 컴포넌트

### 1. 엔티티 (Entity)

#### Notification.java
```java
@Entity
@Table(name = "notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;
    
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    
    @JdbcTypeCode(SqlTypes.JSONB)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    private Instant deliveredAt;
    private Instant readAt;
}
```

**주요 기능:**
- 알림 상태 관리 (생성, 전송, 읽음)
- 팩토리 메서드를 통한 알림 타입별 생성
- JSONB를 활용한 유연한 페이로드 저장

#### NotificationType.java
```java
public enum NotificationType {
    CHAT,        // 채팅 메시지 알림
    CHATREQ,     // 채팅 요청 알림
    SYSTEM       // 시스템 공지/이벤트 알림
}
```

### 2. 서비스 계층 (Service Layer)

#### NotificationPushService.java
**주요 메서드:**
- `sendChatNotify()`: 채팅 알림 발송
- `sendChatReqNotify()`: 채팅 요청 알림 발송
- `sendSystemNotify()`: 시스템 알림 발송
- `pushUndelivered()`: 미전송 알림 재전송

**핵심 로직:**
1. 알림을 데이터베이스에 저장
2. 수신자의 SSE 연결 상태 확인
3. 온라인 시 실시간 전송, 오프라인 시 저장
4. 미전송 알림은 재연결 시 자동 전송

### 3. SSE 관리 (RedisEmitter.java)

#### 주요 기능:
- **로컬 메모리 관리**: ConcurrentHashMap을 통한 SseEmitter 관리
- **Redis 온라인 상태**: TTL 기반 온라인 상태 추적
- **연결 생명주기**: 완료/타임아웃 시 자동 정리
- **하트비트 지원**: TTL 갱신을 통한 연결 유지

```java
@Component
public class RedisEmitter {
    private final ConcurrentMap<Long, SseEmitter> local = new ConcurrentHashMap<>();
    private static final long TIMEOUT = 1000L * 60 * 60 * 6; // 6시간
    
    public SseEmitter add(Long memberId) { /* SSE 연결 추가 */ }
    public void touch(Long memberId) { /* TTL 갱신 */ }
    public boolean isOnline(Long memberId) { /* 온라인 상태 확인 */ }
}
```

### 4. 컨트롤러 (Controller)

#### NotificationController.java
**엔드포인트:**
- `GET /api/v1/notify/sse`: SSE 연결 구독
- `POST /api/v1/notify/heartbeat`: 하트비트 응답

**연결 프로세스:**
1. 인증된 사용자 확인
2. SseEmitter 생성 및 등록
3. 즉시 ping 전송으로 연결 확인
4. 비동기로 미전송 알림 처리

### 5. 스케줄러 (Scheduler)

#### SsePingScheduler.java
```java
@Scheduled(fixedRate = 15_000) // 15초마다
public void pingAll() {
    emitters.forEach((memberId, emitter) -> {
        try {
            emitter.send(SseEmitter.event()
                    .name("ping")
                    .data("{}"));
        } catch (IOException e) {
            emitters.remove(memberId); // 연결 실패 시 정리
        }
    });
}
```

**목적:**
- ALB idle-timeout (60초) 대비 15초 주기로 ping 전송
- 연결 상태 모니터링 및 자동 정리
- 클라이언트 연결 유지

## 🔄 알림 플로우

### 1. 실시간 알림 전송
```
1. 알림 발생 (채팅, 시스템 등)
2. NotificationPushService.sendXXXNotify() 호출
3. 데이터베이스에 알림 저장
4. 수신자 SSE 연결 상태 확인
5. 온라인 시: 실시간 전송 + deliveredAt 업데이트
6. 오프라인 시: 저장만 (재연결 시 전송)
```

### 2. SSE 연결 및 하트비트
```
1. 클라이언트: GET /api/v1/notify/sse 요청
2. 서버: SseEmitter 생성, RedisEmitter에 등록
3. 서버: 즉시 ping 전송으로 연결 확인
4. 클라이언트: ping 수신 시 POST /api/v1/notify/heartbeat 응답
5. 서버: Redis TTL 갱신 (2분)
6. 스케줄러: 15초마다 ping 전송
7. 클라이언트: 25초마다 하트비트 응답
```

### 3. 오프라인 알림 처리
```
1. 사용자 오프라인 상태에서 알림 발생
2. 알림을 데이터베이스에 저장 (deliveredAt = null)
3. 사용자 재연결 시 pushUndelivered() 호출
4. 미전송 알림 조회 및 전송
5. deliveredAt 업데이트
```

## 📊 데이터 모델

### Notification 테이블
```sql
CREATE TABLE notification (
    id UUID PRIMARY KEY,
    receiver_id BIGINT NOT NULL,
    type VARCHAR(20),
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP
);
```

### Redis 키 구조
```
sse:online:{memberId} = "1" (TTL: 2분)
```

## 🛠️ API 명세

### SSE 연결
```
GET /api/v1/notify/sse
Authorization: Bearer {token}
Content-Type: text/event-stream

Response:
- event: ping, data: {}
- event: chat, data: {chatRoomId, senderId, messageId, message}
- event: chatreq, data: {requesterId, requesterName, message}
- event: system, data: {title, content, category}
```

### 하트비트
```
POST /api/v1/notify/heartbeat
Authorization: Bearer {token}

Response: 200 OK
```

### 테스트 API
```
POST /api/test/notifications/send-chat
POST /api/test/notifications/send-chatreq
POST /api/test/notifications/send-system
GET /api/test/notifications/status/{memberName}
```

## 🔧 설정 및 환경

### application.yml 설정
```yaml
spring:
  redis:
    host: localhost
    port: 6379
  
  datasource:
    url: jdbc:postgresql://localhost:5432/howareyou
    
logging:
  level:
    org.example.howareyou.domain.notification: DEBUG
```

### Docker Compose
```yaml
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
  
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: howareyou
    ports:
      - "5432:5432"
```

## 🧪 테스트 방법

### 1. 정적 HTML 테스트
- URL: `http://localhost:8080/notification-test.html`
- 브라우저에서 직접 접근 가능
- SSE 연결 및 알림 발송 테스트

### 2. API 테스트
```bash
# SSE 연결
curl -N -H "Authorization: Bearer {token}" \
     http://localhost:8080/api/v1/notify/sse

# 채팅 알림 발송
curl -X POST http://localhost:8080/api/test/notifications/send-chat \
     -H "Content-Type: application/json" \
     -d '{
       "receiverName": "testuser1",
       "roomId": 1,
       "senderId": 2,
       "messageId": "msg123",
       "message": "안녕하세요!"
     }'
```

## 🚀 성능 및 확장성

### 현재 성능
- **SSE 타임아웃**: 6시간
- **Ping 주기**: 15초
- **Redis TTL**: 2분
- **하트비트 주기**: 25초

### 확장성 고려사항
1. **로드 밸런서**: ALB/NGINX 설정으로 다중 인스턴스 지원
2. **Redis Cluster**: 대용량 사용자 지원을 위한 Redis 클러스터링
3. **데이터베이스**: 알림 히스토리 관리를 위한 파티셔닝
4. **모니터링**: Prometheus + Grafana를 통한 실시간 모니터링

## 🐛 문제 해결

### 일반적인 문제들
1. **CORS 오류**: 프론트엔드 도메인 허용 설정 확인
2. **연결 실패**: Redis 서버 상태 및 포트 확인
3. **알림 수신 안됨**: SSE 연결 상태 및 브라우저 개발자 도구 확인
4. **메모리 누수**: SseEmitter 자동 정리 로직 확인

### 로그 확인
```bash
# 애플리케이션 로그
tail -f logs/application.log | grep notification

# Redis 연결 확인
redis-cli ping

# 데이터베이스 연결 확인
psql -h localhost -U postgres -d howareyou
```

## 📈 모니터링 지표

### 주요 지표
- SSE 연결 수
- 알림 전송 성공률
- Redis 메모리 사용량
- 데이터베이스 쿼리 성능
- 오프라인 알림 저장 수

### 알림 지표
- 알림 타입별 발송 수
- 실시간 vs 오프라인 전송 비율
- 사용자별 알림 수신 패턴

## 🔮 향후 개선 계획

### 단기 개선사항
1. **알림 읽음 처리**: 읽음 상태 업데이트 API
2. **알림 히스토리**: 과거 알림 조회 API
3. **알림 설정**: 사용자별 알림 설정 관리

### 장기 개선사항
1. **푸시 알림**: FCM을 통한 모바일 푸시 알림
2. **알림 그룹화**: 유사한 알림 그룹화 및 요약
3. **AI 기반 알림**: 사용자 패턴 기반 알림 최적화
4. **다국어 지원**: 국제화된 알림 메시지

## 📚 참고 자료

- [Server-Sent Events MDN](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
- [Spring WebFlux SSE](https://docs.spring.io/spring-framework/reference/web/webflux/controller/ann-methods/sse.html)
- [Redis Pub/Sub](https://redis.io/docs/manual/pubsub/)
- [PostgreSQL JSONB](https://www.postgresql.org/docs/current/datatype-json.html) 