import http from 'k6/http';
import { check, sleep, fail } from 'k6';

export const options = {
    scenarios: {
        detail: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.HOT_RPS || 10),
            timeUnit: '1s',
            duration: __ENV.DURATION || '3m',
            preAllocatedVUs: Number(__ENV.PRE_VUS || 50),
            maxVUs: Number(__ENV.MAX_VUS || 100),
        },
    },
    thresholds: {
        'http_req_failed{api:quiz}': ['rate<0.05'],
        'http_req_duration{type:detail}': ['p(95)<2000'],
    },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080/api/test/quiz';
const MEMBERNAME = __ENV.MEMBERNAME || 'user1';

export function setup() {
    const perPage = 5000;
    const totalPages = 2; // 1만건만 수집
    let all = [];

    for (let p = 0; p < totalPages; p++) {
        const url = `${BASE}/uuids?membername=${encodeURIComponent(MEMBERNAME)}&limit=${perPage}&page=${p}`;
        const r = http.get(url, { tags: { api: 'quiz', type: 'scan' } });
        if (!r || r.status !== 200) fail(`GET /uuids failed (page=${p}): ${r && r.status}`);

        let arr = [];
        try { arr = JSON.parse(String(r.body || '[]')); } catch (_) { arr = []; }

        all = all.concat(arr);
    }

    console.log(`✅ Total UUIDs collected for detail: ${all.length}`);
    return { uuids: all };
}

export default function (data) {
    const uuids = (data && data.uuids && data.uuids.length > 0) ? data.uuids : [];
    if (uuids.length === 0) fail('No UUIDs in setup data');
    const u = uuids[Math.floor(Math.random() * uuids.length)];
    const r = http.get(`${BASE}/${u}`, { tags: { api: 'quiz', type: 'detail' } });
    check(r, { 'detail 200': (res) => res && res.status === 200 });
    sleep(Math.random() * 0.05);
}