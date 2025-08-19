# K6 부하 테스트 환경

## 📂 디렉토리 구조
```
server/
└─ k6-test/
   ├─ scripts/
   │   ├─ test.js        # 가장 기본 테스트
   │   ├─ features/…     # 기능별 시나리오 (옵션)
   │   └─ master.js      # 여러 시나리오 종합 (옵션)
   └─ docker-compose.k6.yml
```

---

## ⚙️ docker-compose.k6.yml
```yaml
services:
  k6:
    image: grafana/k6:latest
    container_name: k6-local
    working_dir: /work
    volumes:
      - ./scripts:/work/scripts    # 로컬 scripts/를 컨테이너 /work/scripts에 마운트
    ports:
      - "5665:5665"                # Web Dashboard
    environment:
      - K6_WEB_DASHBOARD=true
      - BASE_URL=http://host.docker.internal:8080
    entrypoint: ["sleep", "infinity"]
```

---

## 🧪 test.js
```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 10,            // 동시에 돌릴 가상 유저 수
    duration: '30s',    // 테스트 시간
    thresholds: {
        http_req_failed: ['rate<0.01'],    // 실패율 < 1%
        http_req_duration: ['p(95)<500'],  // 95% 응답속도 < 500ms
    },
};

export default function () {
    const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
    const res = http.get(`${baseUrl}/actuator/health`);
    check(res, { 'status is 200': (r) => r.status === 200 });
    sleep(1);
}
```

---

## 🚀 실행 방법

### 1) k6 컨테이너 띄우기
```powershell
docker compose -f docker-compose.k6.yml up -d
```

### 2) 컨테이너 안에서 스크립트 실행
```powershell
docker compose -f docker-compose.k6.yml exec k6 k6 run scripts/test.js
```

- 실행 중에는 Web Dashboard 접속 가능  
  👉 [http://127.0.0.1:5665](http://127.0.0.1:5665)

---

## 🎯 기능별 실행 예시
```powershell
docker compose -f docker-compose.k6.yml exec k6 k6 run scripts/features/chat.js
```
