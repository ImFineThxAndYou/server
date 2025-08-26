import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        cold_page: {
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
        'http_req_duration{type:list_cold}': ['p(95)<4000'],
    },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080/api/test/quiz';
const MEMBERNAME = __ENV.MEMBERNAME || 'user1';

function randint(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min; }

export default function () {
    const maxPage = Number(__ENV.MAX_PAGE || 80000);
    const page = randint(Math.floor(maxPage * 0.6), maxPage);
    const status = Math.random() < 0.5 ? 'SUBMIT' : 'PENDING';
    const r = http.get(`${BASE}/me?membername=${encodeURIComponent(MEMBERNAME)}&page=${page}&size=20&status=${status}`, {
        tags: { api: 'quiz', type: 'list_cold' }
    });
    check(r, { 'cold 2xx-ish': (res) => res && [200, 204, 206].includes(res.status) });
    sleep(Math.random() * 0.1);
}