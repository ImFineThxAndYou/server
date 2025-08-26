import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        hot_page: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.HOT_RPS || 10),
            timeUnit: '1s',
            duration: __ENV.DURATION || '5m',
            preAllocatedVUs: Number(__ENV.PRE_VUS || 50),
            maxVUs: Number(__ENV.MAX_VUS || 100),
        },
    },
    thresholds: {
        'http_req_failed{api:quiz}': ['rate<0.05'],
        'http_req_duration{type:list_hot}': ['p(95)<2000'],
    },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080/api/test/quiz';
const MEMBERNAME = __ENV.MEMBERNAME || 'user1';

export default function () {
    const status = Math.random() < 0.5 ? 'SUBMIT' : 'PENDING';
    const r = http.get(`${BASE}/me?membername=${encodeURIComponent(MEMBERNAME)}&page=0&size=20&status=${status}`, {
        tags: { api: 'quiz', type: 'list_hot' }
    });
    check(r, { 'hot 200': (res) => res && res.status === 200 });
    sleep(Math.random() * 0.05);
}