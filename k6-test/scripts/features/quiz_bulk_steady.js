// /work/scripts/features/quiz_bulk_steady.js
import http from 'k6/http';
import { check, sleep } from 'k6';

// 관측용 허용 상태코드(일부 4xx 포함)
http.setResponseCallback(
    http.expectedStatuses(200, 201, 202, 204, 206, 400, 401, 403, 404, 409, 422, 429)
);

// ---------- 옵션 ----------
export const options = {
    scenarios: {
        steady: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.RPS || 400), // 전체 RPS
            timeUnit: '1s',
            duration: __ENV.DURATION || '30s',
            preAllocatedVUs: Number(__ENV.PRE_VUS || 80),
            maxVUs: Number(__ENV.MAX_VUS || 400),
        },
    },
    thresholds: {
        'http_req_failed{api:quiz}': ['rate<0.05'],
        'http_req_duration{type:start}': ['p(95)<2500'],
        'http_req_duration{type:submit}': ['p(95)<2500'],
    },
};

// ---------- 유틸 ----------
function pick(arr) { return arr[Math.floor(Math.random() * arr.length)]; }

const BASE = (__ENV.BASE_URL && __ENV.BASE_URL.trim()) || 'http://localhost:8080/api/test/quiz';
const MEMBERNAME = (__ENV.MEMBERNAME && __ENV.MEMBERNAME.trim()) || 'user1';

function loadTokens() {
    try {
        if (__ENV.TOKENS_FILE && __ENV.TOKENS_FILE.length > 0) {
            const t = open(__ENV.TOKENS_FILE).split('\n').map(s=>s.trim()).filter(Boolean);
            if (t.length) return t;
        }
    } catch (_) {}
    if (__ENV.TOKENS && __ENV.TOKENS.length > 0) {
        const t = __ENV.TOKENS.split(',').map(s=>s.trim()).filter(Boolean);
        if (t.length) return t;
    }
    return [''];
}
const TOKENS = loadTokens();

function H(token, extraTag) {
    const headers = { 'Content-Type': 'application/json' };
    if (token && token.length > 0) headers['Authorization'] = `Bearer ${token}`;
    return { headers, tags: Object.assign({ api: 'quiz' }, extraTag || {}) };
}

function tag(resp, extra) {
    if (!resp) return;
    const type =
        (resp.request && resp.request.params && resp.request.params.tags && resp.request.params.tags.type) ||
        (extra && extra.type) || 'unknown';
    if (resp.status >= 400) {
        console.error(`[${type}] ${resp.status} ${String(resp.body || '').slice(0, 180)}`);
    }
}

/** /me 목록에서 uuid 찾아 문항수(=totalQuestions 또는 배열 길이) 1회만 확보 */
function getCountFromMeOnce(uuid, token) {
    const url = `${BASE}/me?membername=${encodeURIComponent(MEMBERNAME)}&page=0&size=20&status=PENDING`;
    const resp = http.get(url, H(token, { type: 'me' }));
    tag(resp, { type: 'me' });
    if (!resp || resp.status !== 200) return null;

    try {
        const body = JSON.parse(String(resp.body || 'null'));
        const items = Array.isArray(body?.content) ? body.content
            : Array.isArray(body?.items) ? body.items
                : Array.isArray(body) ? body : [];
        const hit = items.find((it) => (it.uuid || it.quizUUID) === uuid);
        if (!hit) return null;

        if (Number.isInteger(hit.totalQuestions)) return hit.totalQuestions;
        if (Array.isArray(hit.questions)) return hit.questions.length;
        if (Array.isArray(hit.quizQuestions)) return hit.quizQuestions.length;
        return null;
    } catch (_) { return null; }
}

/** 상세(대기중 퀴즈 조회)에서 문항수 확보: GET /{uuid}?membername=... */
function getCountFromDetailOnce(uuid, token) {
    const url = `${BASE}/${encodeURIComponent(uuid)}?membername=${encodeURIComponent(MEMBERNAME)}`;
    const resp = http.get(url, H(token, { type: 'detail' }));
    tag(resp, { type: 'detail' });
    if (!resp || resp.status !== 200) return null;

    try {
        const j = JSON.parse(String(resp.body || 'null'));
        if (Array.isArray(j?.quizQuestions)) return j.quizQuestions.length;
        if (Array.isArray(j?.questions)) return j.questions.length;
        return null;
    } catch (_) { return null; }
}

// ---------- 시나리오 본문 ----------
export default function () {
    const token = pick(TOKENS);

    // 1) 50% 데일리 / 50% 랜덤
    const doDaily = Math.random() < 0.5;
    let startResp;
    if (doDaily) {
        const days = ['2025-08-09', '2025-08-10', '2025-08-12', '2025-08-13', '2025-08-14'];
        const d = pick(days);
        startResp = http.post(
            `${BASE}/daily/start?membername=${encodeURIComponent(MEMBERNAME)}&date=${d}`,
            null,
            H(token, { type: 'start' })
        );
    } else {
        const levels = [null, 'A', 'B', 'C']; // 필요 시 서버 Enum에 맞춰 조정
        const lv = pick(levels);
        const qs = [`membername=${encodeURIComponent(MEMBERNAME)}`];
        if (lv) qs.push(`level=${lv}`);
        startResp = http.post(`${BASE}/random/start?` + qs.join('&'), null, H(token, { type: 'start' }));
    }
    tag(startResp, { type: 'start' });
    check(startResp, {
        'start ok (2xx/409/422/429)': (r) => r && (
            (r.status >= 200 && r.status < 300) || r.status === 409 || r.status === 422 || r.status === 429
        ),
    });

    // 2) start 응답에서 uuid 확보 + 문항 수 확보 시도
    let startUuid = null;
    let q = null;
    try {
        const j = JSON.parse(String(startResp.body || 'null'));
        startUuid = j && (j.quizUUID || j.uuid);
        if (Array.isArray(j?.quizQuestions)) q = j.quizQuestions.length;
    } catch (e) {
        console.error('JSON parse error', e);
    }

    // 3) /me → (없으면) /{uuid} 상세로 문항 수 보정
    if (startUuid) {
        const fromMe = getCountFromMeOnce(startUuid, token);
        if (Number.isInteger(fromMe) && fromMe > 0) q = fromMe;

        if (!Number.isInteger(q) || q < 1) {
            const fromDetail = getCountFromDetailOnce(startUuid, token);
            if (Number.isInteger(fromDetail) && fromDetail > 0) q = fromDetail;
        }

        // 방어 기본값
        if (!Number.isInteger(q) || q < 1) q = 5;

        // ✅ 서버는 1-based 선택지 기대
        const answersOneBased = Array.from({ length: q }, () => 1 + Math.floor(Math.random() * 4));

        const payload = JSON.stringify({ selected: answersOneBased });

        const sub = http.post(
            `${BASE}/${encodeURIComponent(startUuid)}/submit`,
            payload,
            H(token, { type: 'submit' }),
        );
        tag(sub, { type: 'submit' });

// ✅ 제출 결과만 출력
        console.log('submitResp', sub.status, String(sub.body || '').slice(0, 200));

        check(sub, {
            'submit ok (200/409/4xx)': (r) => r && [200, 409, 400, 401, 403, 404, 422].includes(r.status),
        });
    }

    sleep(Math.random() * 0.05);
}