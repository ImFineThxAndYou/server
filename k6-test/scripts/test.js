import http from 'k6/http';
import { check, sleep } from 'k6';

// 1) 테스트 옵션 설정
export const options = {
    vus: 10,            // 동시에 돌릴 가상 유저(Virtual Users) 수 = 10명
    duration: '30s',    // 테스트 지속 시간 = 30초
    thresholds: {       // 성능 기준(임계치)
        http_req_failed: ['rate<0.01'],    // 요청 실패율 < 1%
        http_req_duration: ['p(95)<500'],  // 응답시간 95% < 500ms
    },
};

// 2) VU(가상 유저)가 실행하는 함수
export default function () {
    // BASE_URL 환경변수 없으면 localhost:8080 사용
    const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

    // Spring Boot Actuator 헬스체크 엔드포인트 호출
    const res = http.get(`${baseUrl}/actuator/health`);

    // 응답 코드가 200인지 확인
    check(res, { 'status is 200': (r) => r.status === 200 });

    // 1초 쉬었다가 다음 루프 실행
    sleep(1);
}
