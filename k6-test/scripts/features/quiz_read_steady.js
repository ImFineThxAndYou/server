// /work/scripts/features/quiz_read_steady.js
import http from 'k6/http';
import { check, sleep, fail } from 'k6';

// ---------- 옵션 ----------
export const options = {
    scenarios: {
        hot_page: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.HOT_RPS || 300),
            timeUnit: '1s',
            duration: __ENV.DURATION || '10s',
            preAllocatedVUs: Number(__ENV.PRE_VUS || 60),
            maxVUs: Number(__ENV.MAX_VUS || 300),
            exec: 'hotPage',
        },
        cold_page: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.COLD_RPS || 200),
            timeUnit: '1s',
            duration: __ENV.DURATION || '10s',
            preAllocatedVUs: Number(__ENV.PRE_VUS || 60),
            maxVUs: Number(__ENV.MAX_VUS || 300),
            exec: 'coldPage',
        },
        detail: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.DETAIL_RPS || 250),
            timeUnit: '1s',
            duration: __ENV.DURATION || '10s',
            preAllocatedVUs: Number(__ENV.PRE_VUS || 60),
            maxVUs: Number(__ENV.MAX_VUS || 300),
            exec: 'detailOne',
        },
    },
    thresholds: {
        'http_req_failed{api:quiz}': ['rate<0.05'],
        'http_req_duration{type:list_hot}': ['p(95)<2000'],
        'http_req_duration{type:list_cold}': ['p(95)<4000'],
        'http_req_duration{type:detail}': ['p(95)<2000'],
    },
};

// ---------- 유틸 (외부 모듈 없이) ----------
function pick(arr) { return arr[Math.floor(Math.random() * arr.length)]; }
function randint(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min; }

const BASE = (__ENV.BASE_URL && __ENV.BASE_URL.trim()) || 'http://localhost:8080/api/test/quiz';
const MEMBERNAME = (__ENV.MEMBERNAME && __ENV.MEMBERNAME.trim()) || 'user1';

// 토큰(옵션)
function loadTokens() {
    try {
        if (__ENV.TOKENS_FILE && __ENV.TOKENS_FILE.length > 0) {
            const t = open(__ENV.TOKENS_FILE).split('\n').map(s => s.trim()).filter(Boolean);
            if (t.length) return t;
        }
    } catch (_) {}
    if (__ENV.TOKENS && __ENV.TOKENS.length > 0) {
        const t = __ENV.TOKENS.split(',').map(s => s.trim()).filter(Boolean);
        if (t.length) return t;
    }
    return ['']; // 토큰 없이도 동작
}
const TOKENS = loadTokens();
function H(token) {
    const headers = { 'Content-Type': 'application/json' };
    if (token && token.length > 0) headers['Authorization'] = `Bearer ${token}`;
    return { headers, tags: { api: 'quiz' } };
}
function tag(resp, extra) {
    if (!resp) return;
    // 요청에 넣은 태그를 읽어오되, 못 읽으면 extra.type 또는 'unknown'
    const reqType =
        (resp.request && resp.request.params && resp.request.params.tags && resp.request.params.tags.type) ||
        (extra && extra.type) ||
        'unknown';

    if (resp.status >= 400) {
        console.error(`[${reqType}] ${resp.status} ${String(resp.body || '').slice(0, 180)}`);
    }
}

// ---------- setup: UUID 풀 ----------
export function setup() {
    // 1) 파일이 있으면 우선
    if (__ENV.UUIDS_FILE && __ENV.UUIDS_FILE.length > 0) {
        try {
            const arr = open(__ENV.UUIDS_FILE).split('\n').map(s => s.trim()).filter(Boolean);
            if (arr.length === 0) fail('UUIDS_FILE is empty');
            return { uuids: arr };
        } catch (e) {
            console.error('Failed to open UUIDS_FILE:', e && e.message ? e.message : e);
            // fallthrough
        }
    }

    // 2) 테스트 엔드포인트에서 직접 가져오기
    const token = (function () {
        // 토큰 옵션 (없어도 동작)
        try {
            if (__ENV.TOKENS_FILE && __ENV.TOKENS_FILE.length > 0) {
                const t = open(__ENV.TOKENS_FILE).split('\n').map(s => s.trim()).filter(Boolean);
                if (t.length) return t[0];
            }
        } catch (_) {}
        if (__ENV.TOKENS && __ENV.TOKENS.length > 0) {
            const t = __ENV.TOKENS.split(',').map(s => s.trim()).filter(Boolean);
            if (t.length) return t[0];
        }
        return ''; // no token
    })();

    const headers = { 'Content-Type': 'application/json' };
    if (token && token.length > 0) headers['Authorization'] = `Bearer ${token}`;

    const limit = Number(__ENV.SCAN_MAX_UUIDS || 150000);
    const url = `${BASE}/uuids?membername=${encodeURIComponent(MEMBERNAME)}&limit=${limit}`; // status 필요하면 &status=SUBMIT

    const r = http.get(url, { headers, tags: { api: 'quiz', type: 'scan' } });
    if (!r || r.status !== 200) fail(`GET /uuids failed: ${r && r.status}`);

    let arr = [];
    try { arr = JSON.parse(String(r.body || '[]')); } catch (_) { arr = []; }

    if (!Array.isArray(arr) || arr.length === 0) fail('No UUIDs returned from /uuids');
    console.log(`Collected UUIDs from /uuids: ${arr.length}`);
    return { uuids: arr };
}

// ---------- execs ----------
export function hotPage() {
    const token = pick(TOKENS);
    const status = Math.random() < 0.5 ? 'SUBMIT' : 'PENDING';
    const r = http.get(`${BASE}/me?membername=${encodeURIComponent(MEMBERNAME)}&page=0&size=20&status=${status}`, H(token));
    tag(r, { type: 'list_hot' });
    check(r, { 'hot 200': (res) => res && res.status === 200 });
    sleep(Math.random() * 0.05);
}

export function coldPage() {
    const token = pick(TOKENS);
    const maxPage = Number(__ENV.MAX_PAGE || 80000);
    const page = randint(Math.floor(maxPage * 0.6), maxPage);
    const status = Math.random() < 0.5 ? 'SUBMIT' : 'PENDING';
    const r = http.get(`${BASE}/me?membername=${encodeURIComponent(MEMBERNAME)}&page=${page}&size=20&status=${status}`, H(token));
    tag(r, { type: 'list_cold' });
    check(r, { 'cold 2xx-ish': (res) => res && [200, 204, 206].includes(res.status) });
    sleep(Math.random() * 0.1);
}

export function detailOne(data) {
    const token = pick(TOKENS);
    const uuids = (data && data.uuids && data.uuids.length > 0) ? data.uuids : [];
    if (uuids.length === 0) fail('No UUIDs in setup data');
    const u = pick(uuids);
    const r = http.get(`${BASE}/${u}`, H(token));
    tag(r, { type: 'detail' });
    check(r, { 'detail 200': (res) => res && res.status === 200 });
    sleep(Math.random() * 0.05);
}