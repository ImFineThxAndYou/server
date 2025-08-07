package org.example.howareyou.domain.notification.redis;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisEmitter {
    private final RedisTemplate<String, String> redis;
    private final ConcurrentMap<Long, SseEmitter> local = new ConcurrentHashMap<>();
    private static final long TIMEOUT = 1000L * 60 * 60 * 6; // 6h

    /* ---------- emitter 관리 ---------- */
    public SseEmitter add(Long memberId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        local.put(memberId, emitter);
        touch(memberId); // online flag set

        emitter.onCompletion(() -> remove(memberId));
        emitter.onTimeout(()   -> remove(memberId));
        return emitter;
    }

    public Optional<SseEmitter> get(Long memberId) {
        return Optional.ofNullable(local.get(memberId));
    }

    public void remove(Long memberId) {
        local.remove(memberId);
        redis.delete("sse:online:" + memberId);
    }

    /* ---------- heartbeat용 TTL 연장 ---------- */
    public void touch(Long memberId) {
        redis.opsForValue().set("sse:online:" + memberId, "1",
                Duration.ofMinutes(2)); // heartbeat 주기의 2배 정도
    }

    public boolean isOnline(Long memberId) {
        return "1".equals(redis.opsForValue().get("sse:online:" + memberId));
    }

    /* 유틸: for-each 순회 */
    public void forEach(java.util.function.BiConsumer<Long,SseEmitter> fn) {
        local.forEach(fn);
    }
}