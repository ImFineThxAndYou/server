# 🔐 HowAreYou 로그인 테스트 가이드

## 📋 개요

이 문서는 HowAreYou 프로젝트의 로그인 시스템을 테스트하는 방법을 설명합니다.

## 🚀 빠른 시작

### 1. 애플리케이션 실행
```bash
# 개발 환경으로 실행
./gradlew bootRun
```

### 2. 테스트 페이지 접속
브라우저에서 다음 URL로 접속:

**메인 테스트 페이지:**
```
http://localhost:8080/test-login.html
```

**테스트 환경 정보:**
```
http://localhost:8080/test-info.html
```

**API 문서:**
```
http://localhost:8080/swagger-ui.html
```

## 🧪 테스트 방법

### 방법 1: HTML 테스트 페이지 (추천)

**장점:**
- 직관적인 UI
- 실시간 토큰 확인
- API 테스트 기능 내장
- 브라우저에서 바로 사용 가능

**사용법:**
1. `http://localhost:8080/test-login.html` 접속
2. 테스트 계정으로 로그인
3. 토큰 정보 확인
4. API 테스트 버튼으로 다양한 엔드포인트 테스트

### 방법 2: API 직접 호출

**테스트 계정 정보:**
```json
{
  "test@example.com": "password123!",
  "admin@example.com": "admin123!",
  "user1@example.com": "user123!",
  "user2@example.com": "user123!"
}
```

**로그인 API 호출:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123!"
  }'
```

**토큰 갱신:**
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Cookie: Refresh=YOUR_REFRESH_TOKEN"
```

### 방법 3: 테스트용 API 엔드포인트

**테스트 로그인:**
```bash
curl -X POST http://localhost:8080/api/test/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123!"
  }'
```

**현재 사용자 정보:**
```bash
curl -X GET http://localhost:8080/api/test/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**토큰 유효성 검사:**
```bash
curl -X GET http://localhost:8080/api/test/token/validate \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 📊 테스트 계정 상세 정보

| 이메일 | 비밀번호 | 역할 | 프로필 완성 | 설명 |
|--------|----------|------|-------------|------|
| test@example.com | password123! | USER | ✅ | 기본 테스트 계정 |
| admin@example.com | admin123! | ADMIN | ✅ | 관리자 계정 |
| user1@example.com | user123! | USER | ✅ | 일반 사용자 |
| user2@example.com | user123! | USER | ❌ | 프로필 미완성 사용자 |

## 🔧 개발 환경 설정

### 환경 변수 설정 (.env 파일)
```env
# 데이터베이스
DB_HOST=localhost
DB_PORT=5432
DB_NAME=howareyou_dev
DB_USERNAME=postgres
DB_PASSWORD=password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# MongoDB
MONGO_HOST=localhost
MONGO_PORT=27017
MONGO_DB=howareyou_dev

# JWT
JWT_SECRET=your-super-secret-jwt-key-here

# Google OAuth2 (선택사항)
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# 프론트엔드 URL
FRONT_URL=http://localhost:3000
```

### Docker Compose로 인프라 실행
```bash
docker-compose up -d
```

## 🧪 API 테스트 시나리오

### 1. 기본 로그인 플로우
1. 로그인 API 호출
2. Access Token과 Refresh Token 받기
3. 보호된 엔드포인트 접근 테스트
4. 토큰 갱신 테스트
5. 로그아웃 테스트

### 2. 에러 케이스 테스트
1. 잘못된 비밀번호로 로그인 시도
2. 존재하지 않는 계정으로 로그인 시도
3. 만료된 토큰으로 API 호출
4. 토큰 없이 보호된 엔드포인트 접근

### 3. OAuth2 소셜 로그인 테스트
1. Google OAuth2 로그인 플로우
2. 신규 사용자 자동 생성 확인
3. 기존 사용자 로그인 확인

## 🔍 디버깅 팁

### 로그 확인
```bash
# 애플리케이션 로그 확인
tail -f logs/application.log

# 특정 패키지 로그 레벨 조정
logging.level.org.example.howareyou=DEBUG
```

### 토큰 디코딩
JWT 토큰을 디코딩하려면 [jwt.io](https://jwt.io) 사이트를 사용하세요.

### 데이터베이스 확인
```sql
-- 테스트 계정 확인
SELECT * FROM auth WHERE provider = 'LOCAL';

-- 사용자 정보 확인
SELECT m.email, m.membername, mp.nickname, mp.completed 
FROM member m 
JOIN member_profile mp ON m.id = mp.member_id;
```

## 🔒 보안 설정

### 개발 환경 보안 설정
- **프로필 기반 접근 제어**: `dev` 프로필에서만 테스트 경로 접근 허용
- **자동 비활성화**: 프로덕션 환경에서는 테스트 기능 자동 비활성화
- **로그 기록**: 보안 설정 변경 시 로그 기록

### 허용된 테스트 경로
```
/test-login.html          # 로그인 테스트 페이지
/test-info.html           # 테스트 환경 정보 페이지
/api/test/**              # 테스트용 API 엔드포인트
/test/**                  # 테스트 관련 정적 리소스
/dev/**                   # 개발 도구
/debug/**                 # 디버깅 도구
/h2-console/**            # H2 데이터베이스 콘솔 (개발용)
/actuator/**              # Spring Actuator (개발용)
```

## 🚨 주의사항

1. **테스트 계정은 개발 환경에서만 사용하세요**
2. **프로덕션 환경에서는 테스트 기능이 비활성화됩니다**
3. **실제 사용자 데이터와 혼동하지 마세요**
4. **토큰은 안전하게 관리하고 공유하지 마세요**
5. **개발 환경에서만 테스트 경로에 접근하세요**

## 📚 추가 리소스

- [Swagger UI](http://localhost:8080/swagger-ui.html) - API 문서
- [Spring Security 공식 문서](https://docs.spring.io/spring-security/reference/)
- [JWT 공식 문서](https://jwt.io/introduction/)

## 🤝 문제 해결

### 자주 발생하는 문제

**1. 로그인 실패**
- 데이터베이스 연결 확인
- 테스트 계정이 생성되었는지 확인
- 비밀번호가 정확한지 확인

**2. 토큰 갱신 실패**
- Refresh Token이 유효한지 확인
- 쿠키 설정이 올바른지 확인

**3. 보호된 엔드포인트 접근 실패**
- Authorization 헤더가 올바르게 설정되었는지 확인
- 토큰이 만료되지 않았는지 확인

### 지원

문제가 발생하면 다음을 확인하세요:
1. 애플리케이션 로그
2. 브라우저 개발자 도구 (Network 탭)
3. 데이터베이스 상태
4. Redis 연결 상태 