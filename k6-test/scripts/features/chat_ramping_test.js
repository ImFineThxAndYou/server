import http from "k6/http";
import ws from "k6/ws";
import { sleep } from "k6";
import { randomIntBetween } from "https://jslib.k6.io/k6-utils/1.4.0/index.js";

export function setup() {
    // ✅ Spring Boot API로부터 채팅방 UUID 전체 조회
    const res = http.get("http://host.docker.internal:8080/api/test/chat/rooms");
    if (res.status !== 200) {
        throw new Error(`❌ 채팅방 UUID 조회 실패: status=${res.status}`);
    }
    const rooms = JSON.parse(res.body);
    // console.log(`✅ 불러온 채팅방 개수: ${rooms.length}`);
    return rooms;
}

export const options = {
    scenarios: {
        ramping: {
            executor: "ramping-arrival-rate",
            startRate: 10,
            timeUnit: "1s",
            preAllocatedVUs: 50,
            maxVUs: 500,
            stages: [
                { target: 100, duration: "30s" },
                { target: 300, duration: "1m" },
                { target: 500, duration: "1m30s" },
            ],
        },
    },
};

export default function (rooms) {
    const userId = randomIntBetween(1, 5000);
    const membername = `member${userId}`;
    const chatRoomUuid = rooms[randomIntBetween(0, rooms.length - 1)];

    const message = {
        chatRoomUuid,
        content: `hello from ${membername}`,
        membername,
    };

    const url = "ws://host.docker.internal:8080/ws-chatroom";

    ws.connect(url, {}, function (socket) {
        socket.on("open", () => {
            // 1. CONNECT
            const connectFrame =
                "CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n\n\0";
            socket.send(connectFrame);
            // console.log(`🔗 Sent CONNECT from ${membername}`);
        });

        // 2. 서버 메시지 수신
        socket.on("message", (msg) => {
            // console.log("📩 Received:", msg);

            if (msg.includes("CONNECTED")) {
                // 3. SUBSCRIBE (선택적)
                const subscribeFrame =
                    "SUBSCRIBE\nid:sub-0\ndestination:/topic/chat.rampingtest\n\n\0";
                socket.send(subscribeFrame);
                // console.log(`📡 Subscribed to /topic/chat.rampingtest`);

                // 4. SEND
                const body = JSON.stringify(message);
                const sendFrame =
                    "SEND\ndestination:/app/chat.rampingtest.send\ncontent-type:application/json\ncontent-length:" +
                    body.length +
                    "\n\n" +
                    body +
                    "\0";

                socket.send(sendFrame);
                // console.log(`📨 Sent message from ${membername} to room=${chatRoomUuid}`);
            }
        });

        // 5. 일정 시간 대기 후 종료
        socket.setTimeout(() => {
            socket.close();
        }, 1000);
    });
}