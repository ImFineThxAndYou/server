import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

export const options = {
    scenarios: {
        hourly_job: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 50 },   // Ramp-up (1분) → 0명에서 50명까지 증가
                { duration: '3m', target: 50 },   // Steady (3분) → 50명 유지
                { duration: '2m', target: 100 },  // Stress (2분) → 50명에서 100명까지 증가
                { duration: '30s', target: 0 },   // Cooldown (30초) → 100명에서 0명까지 감소
                //총 6m30s 소요
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<3000'], // 95% 요청 3초 이내
        http_req_failed: ['rate<0.01'],    // 실패율 1% 미만
    },
};

export default function () {
    const from = '2025-08-20T07:00:00Z';
    const to   = '2025-08-20T08:00:00Z';
    const url  = `${BASE_URL}/api/test/voca/replay?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`;

    const res = http.post(url, null, { headers: { 'Content-Type': 'application/json' } });

    check(res, {
        'status is 202': (r) => r.status === 202,
    });

    sleep(1); // 사용자 think-time
}
