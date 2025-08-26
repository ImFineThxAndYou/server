import http from 'k6/http';
import { check } from 'k6';

// 1. 테스트 옵션 설정
export const options = {
    // 가상 유저(VU) 1명이 1번만 실행
    vus: 1,
    iterations: 1,

    // 테스트 성공/실패 기준
    thresholds: {
        // 이 테스트는 반드시 성공해야 함
        'http_req_failed': ['rate<0.01'],
        // 예: 전체 작업이 30분(1,800,000ms) 안에는 끝나야 한다
        'http_req_duration': ['p(95)<1800000'],
    },
};

// 2. 실제 테스트 로직
export default function () {
    // 전체 사용자 계산을 시작시키는 API 엔드포인트
    const url = 'http://localhost:8080/api/test/tags/recommend';

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${__ENV.ADMIN_TOKEN}`, // 관리자 토큰 필요
        },
        // 작업이 매우 오래 걸리므로 타임아웃을 넉넉하게 설정 (예: 40분)
        timeout: '2400s',
    };

    // POST 요청으로 전체 계산 작업을 트리거
    const res = http.post(url, null, params);

    // 3. 결과 확인
    check(res, {
        'batch job API returned 200 OK': (r) => r.status === 200,
    });

    // k6 결과에서 http_req_duration 값을 확인하면 총 소요 시간이 나옵니다.
    console.log(`Batch job completed. Total duration: ${res.timings.duration} ms`);
}