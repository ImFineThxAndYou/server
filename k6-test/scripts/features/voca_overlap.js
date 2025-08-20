// scripts/features/voca_overlap.js
import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const FROM = __ENV.FROM || '2025-08-18T05:00:00Z';
const TO   = __ENV.TO   || '2025-08-18T06:00:00Z';

let logged = 0;
const LOG_LIMIT = 5;

export const options = {
    scenarios: {
        overlap: { executor: 'shared-iterations', vus: 5, iterations: 20, maxDuration: '30s' },
    },
    thresholds: {
        http_req_duration: ['p(95)<3000'],
        // 원인 파악 중이면 아래 줄은 주석 처리해두는 것도 방법:
        // http_req_failed: ['rate<0.001'],
    },
};

export default function () {
    const url = `${BASE_URL}/api/test/voca/replay?from=${encodeURIComponent(FROM)}&to=${encodeURIComponent(TO)}`;
    const res = http.post(url, null, { headers: { 'Content-Type': 'application/json' } });

    if (res.status !== 202 && logged < LOG_LIMIT) {
        console.log(`status=${res.status}, body=${res.body}`); // ★ 실패 이유 확인
        logged++;
    }

    check(res, { '202 accepted': (r) => r.status === 202 });
}
