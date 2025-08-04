package org.example.howareyou.domain.member.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberCacheService {

    private static final String   PREFIX   = "member:";
    private static final Duration TTL      = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redis;
    private ValueOperations<String, Object> ops;

    @jakarta.annotation.PostConstruct
    void init() { ops = redis.opsForValue(); }

    /* ---------- CRUD ---------- */

    public void cacheMember(MemberCache mc) {
        if (mc == null) return;
        ops.set(key(Long.parseLong(mc.getId())), mc, TTL);
    }

    public Optional<MemberCache> getCached(Long id) {

        if (id == null) return Optional.empty();
        MemberCache mc = (MemberCache) ops.get(key(id));
        if (mc != null) redis.expire(key(id), TTL);    // TTL 연장
        return Optional.ofNullable(mc);
    }

    public void delete(Long id) { redis.delete(key(id)); }

    /* ---------- Presence ---------- */

    public void updateMemberState(Long id, boolean online) {
        getCached(id).ifPresent(mc -> cacheMember(
                online ? mc.updateAsActive() : mc.updateAsInactive()));
    }

    public boolean isMemberOnline(Long id) {
        return getCached(id).map(MemberCache::isOnline).orElse(false);
    }

    public LocalDateTime getLastActiveAt(Long id) {
        return getCached(id).map(MemberCache::getLastActiveAt).orElse(null);
    }

    /* ---------- util ---------- */
    private String key(Long id) { return PREFIX + id; }
}