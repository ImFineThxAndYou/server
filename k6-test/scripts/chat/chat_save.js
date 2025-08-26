import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// ========= 환경 변수 =========
// 로그인 API 엔드포인트
const LOGIN_ENDPOINT = __ENV.LOGIN_ENDPOINT || '/api/auth/login';
const LOGIN_EMAIL = __ENV.LOGIN_EMAIL || 'test@example.com';
const LOGIN_PASSWORD = __ENV.LOGIN_PASSWORD || 'password123!';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const MESSAGE_ENDPOINT = __ENV.MESSAGE_ENDPOINT || '/api/chat/messages';
const ROOMS_ENDPOINT = __ENV.ROOMS_ENDPOINT || '';
const SENDER_IDS = (__ENV.SENDER_IDS || '1').split(',').map((s) => parseInt(s.trim(), 10));
const MSG_PREFIX = __ENV.MSG_PREFIX || 'k6-msg';

// ========= 커스텀 메트릭 =========
const Latency = new Trend('chat_save_duration');
const Failures = new Counter('chat_save_failures');

// ========= 시나리오/임계값 =========
export const options = {
  scenarios: {
    open_rate: {
      executor: 'ramping-arrival-rate',
      startRate: 50,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 2000,
      stages: [
        { duration: '1m', target: 2000 },  // 2000 rps
      ],
    },
  },

  thresholds: {
    http_req_failed: ['rate<0.01'],
    chat_save_duration: ['p(95)<200'],
    checks: ['rate>0.99'],
  },
};

// ========= setup: 로그인 + 룸 UUID 확보 =========
export function setup() {
  // 1) 로그인
  const loginRes = http.post(`${BASE_URL}${LOGIN_ENDPOINT}`, JSON.stringify({
    email: LOGIN_EMAIL,
    password: LOGIN_PASSWORD,
  }), { headers: { 'Content-Type': 'application/json' } });

  check(loginRes, { 'login 200': (r) => r.status === 200 });
  if (loginRes.status !== 200) {
    throw new Error(`❌ 로그인 실패: status=${loginRes.status}, body=${loginRes.body}`);
  }

  let token;
  try {
    const data = JSON.parse(loginRes.body);
    // 서버가 access/refresh 구조를 반환하므로 access 사용
    token = data.access;
  } catch (e) {
    throw new Error(`❌ 로그인 응답 파싱 실패: ${e.message}`);
  }

  if (!token) {
    throw new Error(`❌ 로그인 성공했지만 access 토큰을 찾을 수 없음: ${loginRes.body}`);
  }


  console.log(`✅ 로그인 성공, 토큰 획득`);

  // 2) 룸 목록 조회
  let roomUuids = [];
  if (ROOMS_ENDPOINT) {
    const res = http.get(`${BASE_URL}${ROOMS_ENDPOINT}`, { headers: { Authorization: `Bearer ${token}` } });
    if (res.status === 200) {
      try {
        const data = JSON.parse(res.body);
        if (Array.isArray(data)) {
          roomUuids = data.map((v) => (typeof v === 'string' ? v : v.uuid)).filter(Boolean);
        } else if (data && Array.isArray(data.rooms)) {
          roomUuids = data.rooms.map((v) => (typeof v === 'string' ? v : v.uuid)).filter(Boolean);
        }
      } catch (e) {
        console.error(`⚠️ rooms 응답 파싱 실패: ${e.message}`);
      }
    }
  }

  if (roomUuids.length === 0 && __ENV.ROOM_UUID) {
    roomUuids = [__ENV.ROOM_UUID];
  }

  if (roomUuids.length === 0) {
    throw new Error('❌ 테스트할 roomUuid가 없습니다. ROOM_UUID 혹은 ROOMS_ENDPOINT를 지정하세요.');
  }

  console.log(`✅ 테스트 룸 선택: ${roomUuids[0]} (총 ${roomUuids.length}개)`);

  return { token, roomUuids };
}

// ========= 메시지 본문 생성 =========
function buildBody(roomUuid, senderId) {
  const clientMessageId = `${senderId}-${__VU}-${__ITER}-${Date.now()}`;
  return JSON.stringify({
    chatRoomUuid: roomUuid,
    senderId,
    content: `${MSG_PREFIX} :: vu=${__VU} iter=${__ITER}`,
    clientMessageId,
  });
}

// ========= 실행 =========
export default function (data) {
  const token = data.token;
  const rooms = data.roomUuids || [];
  const roomUuid = rooms[Math.floor(Math.random() * rooms.length)];

  const senderId = SENDER_IDS[(__ITER + __VU) % SENDER_IDS.length];
  const url = `${BASE_URL}${MESSAGE_ENDPOINT}`;
  const body = buildBody(roomUuid, senderId);
  const params = {
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
    timeout: '5s',
  };

  const res = http.post(url, body, params);

  Latency.add(res.timings.duration);

  const ok = check(res, {
    'status is 200/201': (r) => r.status === 200 || r.status === 201,
    'json ok': (r) => !!r.body,
  });

  if (!ok) {
    Failures.add(1);
    console.error(`❌ 요청 실패: status=${res.status}, body=${res.body}`);
  }

  sleep(0.01);
}
