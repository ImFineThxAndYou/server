import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const DATE = __ENV.DATE || '2025-08-21';

// DB 유저 범위 (200명 중 앞 150명만)
const START_ID = 4622;
const END_ID = 4821; // 4622 + 150 - 1

export const options = {
    scenarios: {
        user_vocab: {
            executor: 'per-vu-iterations',
            vus: 200,          // 사용자 수 (운영에서 예상되는 동시 생성 유저 수)
            iterations: 1,     // 하루 1회만 실행
            maxDuration: '5m', // 전체 테스트 허용 시간
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000'],
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    // 150명 사용자 안에서만 분배
    const vuId = __VU; // 현재 VU 번호 (1~150)
    const userId = START_ID + vuId - 1; // 4622~4771

    const url = `${BASE_URL}/api/test/voca/user-daily?userId=${userId}&date=${DATE}&ignoreTimezone=true`;
    const res = http.post(url, null, { headers: { 'Content-Type': 'application/json' } });

    check(res, { 'status is 200': (r) => r.status === 200 });
    sleep(1);
}
