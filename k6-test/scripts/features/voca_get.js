import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const MEMBER = __ENV.MEMBER || 'user1';
const DATE   = __ENV.DATE   || '2025-08-14';
const LEVEL  = __ENV.LEVEL  || 'a1';

export const options = {
    scenarios: {
        // 1) 전체 단어 조회
        all_words: {
            executor: 'constant-arrival-rate',
            exec: 'getAllWords',
            rate: 100,                // 초당 요청 수 (RPS)
            timeUnit: '1s',           // 기준 단위
            duration: '1m',           // 총 1분 동안 실행
            preAllocatedVUs: 50,      // 최소 VU 수
            maxVUs: 200,              // 최대 VU 수
        },

        // 2) 날짜별 단어장 조회
        date_words: {
            executor: 'constant-arrival-rate',
            exec: 'getWordsByDate',
            rate: 100,
            timeUnit: '1s',
            duration: '1m',
            preAllocatedVUs: 50,
            maxVUs: 200,
        },

        // 3) 난이도별 단어장 조회
        level_words: {
            executor: 'constant-arrival-rate',
            exec: 'getWordsByLevel',
            rate: 100,
            timeUnit: '1s',
            duration: '1m',
            preAllocatedVUs: 50,
            maxVUs: 200,
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<1000'], // 95% 요청이 1초 이내
        http_req_failed:   ['rate<0.01'],  // 실패율 1% 미만
    },
};

// 1) 사용자 전체 단어 조회
export function getAllWords() {
    const url = `${BASE_URL}/api/vocabook/member/${MEMBER}?page=0&size=50`;
    const res = http.get(url);
    console.log(`status=${res.status}, body=${res.body.substring(0,200)}`);
    check(res, { 'status 200': (r) => r.status === 200 });
}

// 2) 특정 날짜 단어장 조회
export function getWordsByDate() {
    const url = `${BASE_URL}/api/vocabook/member/${MEMBER}/${DATE}?page=0&size=50&sortBy=analyzedAt&direction=desc`;
    const res = http.get(url);
    console.log(`status=${res.status}, body=${res.body.substring(0,200)}`);
    check(res, { 'status 200': (r) => r.status === 200 });
}

// 3) 난이도별 단어장 조회
export function getWordsByLevel() {
    const url = `${BASE_URL}/api/vocabook/member/${MEMBER}/level/${LEVEL}?page=0&size=50`;
    const res = http.get(url);
    console.log(`status=${res.status}, body=${res.body.substring(0,200)}`);
    check(res, { 'status 200': (r) => r.status === 200 });
}
