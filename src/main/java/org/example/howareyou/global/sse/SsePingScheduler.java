package org.example.howareyou.global.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.notification.redis.RedisEmitter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class SsePingScheduler {

    private final RedisEmitter emitters;

    /** ALB idle-timeout(60 s) 보다 짧게, 15 s로 단축 */
    @Scheduled(fixedRate = 15_000)
    public void pingAll() {
        emitters.forEach((memberId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("ping")
                        .data("{}"));
            } catch (IOException e) {          // write 실패 → 정리
                emitters.remove(memberId);
            }
        });
    }
}