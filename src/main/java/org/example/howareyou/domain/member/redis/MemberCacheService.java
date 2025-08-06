package org.example.howareyou.domain.member.redis;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * 멤버 캐싱 서비스
 * 
 * 캐싱 로직:
 * 1. 멤버가 로그인/회원가입 하거나 API 요청을 보내면서 JWT 인증 필터를 거칠 때 캐싱
 * 2. TTL을 짧게 가져감 (5분)
 * 3. 캐시에 해당 ID가 없으면 캐싱하고 있으면 touch를 통해 TTL 연장
 * 4. 유저 정보 조회 시 캐시 먼저 확인, 없으면 DB에서 가져감
 * 5. 캐싱된 것으로 해당 사용자가 활성화 상태인지 확인
 * 6. 사용자가 캐싱될 때 lastActiveAt을 DB에 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberCacheService {

    private static final String PREFIX = "member:";
    private static final Duration TTL = Duration.ofMinutes(5); // 짧은 TTL

    private final RedisTemplate<String, Object> redis;
    private final MemberRepository memberRepo;
    private ValueOperations<String, Object> ops;

    @PostConstruct
    void init() {
        ops = redis.opsForValue();
    }

    /**
     * 멤버 캐싱 (로그인/회원가입 시)
     * 캐시에 없으면 새로 캐싱하고, 있으면 touch로 TTL 연장
     */
    public void cache(Member member) {
        if (member == null) return;
        
        Long memberId = member.getId();
        String key = key(memberId);
        
        try {
            // 캐시에 이미 있는지 확인
            Object cached = ops.get(key);
            
            if (cached == null) {
                // 캐시에 없으면 새로 캐싱 (JOIN FETCH로 이미 로드됨)
                MemberCache memberCache = MemberCache.from(member).touch();
                ops.set(key, memberCache, TTL);
                log.debug("새로운 멤버 캐싱: {}", memberId);
            } else {
                // 캐시에 있으면 touch로 TTL 연장
                touch(memberId);
            }
            
            // DB의 lastActiveAt 업데이트
            member.markActiveNow();
            memberRepo.save(member);
            
        } catch (Exception e) {
            log.error("멤버 캐싱 중 오류 발생: {}", memberId, e);
        }
    }

    /**
     * 멤버 ID로 캐싱 (JWT 인증 필터에서 사용)
     */
    public void cache(Long memberId) {
        if (memberId == null) return;
        
        try {
            // Lazy Loading 방지를 위해 JOIN FETCH 사용
            Member member = memberRepo.findByIdWithProfileAndInterests(memberId).orElse(null);
            if (member != null) {
                cache(member);
            }
        } catch (Exception e) {
            log.error("멤버 ID로 캐싱 중 오류 발생: {}", memberId, e);
        }
    }

    /**
     * 캐시 touch (TTL 연장)
     * 60초 이내에 이미 touch했다면 skip
     */
    public void touch(Long memberId) {
        if (memberId == null) return;
        
        String key = key(memberId);
        
        try {
            Object cached = ops.get(key);
            
            if (cached != null) {
                // 캐시가 있으면 TTL 연장
                redis.expire(key, TTL);
                
                // 60초 이내에 이미 touch했다면 DB 업데이트 skip
                if (cached instanceof MemberCache) {
                    MemberCache memberCache = (MemberCache) cached;
                    if (Instant.now().minusSeconds(60).isAfter(memberCache.getLastActiveAt())) {
                        // 60초 이상 지났으면 DB 업데이트
                        memberRepo.updateLastActive(memberId, Instant.now());
                    }
                }
                
                log.debug("멤버 캐시 touch: {}", memberId);
            } else {
                // 캐시가 없으면 새로 캐싱
                cache(memberId);
            }
            
        } catch (Exception e) {
            log.error("멤버 캐시 touch 중 오류 발생: {}", memberId, e);
        }
    }

    /**
     * 캐시에서 멤버 조회
     * ClassCastException 방지를 위해 안전한 캐스팅 사용
     */
    public Optional<MemberCache> get(Long memberId) {
        if (memberId == null) return Optional.empty();
        
        String key = key(memberId);
        
        try {
            Object cached = ops.get(key);
            
            if (cached instanceof MemberCache) {
                return Optional.of((MemberCache) cached);
            } else if (cached != null) {
                // LinkedHashMap으로 deserialized된 경우 처리
                log.debug("캐시된 객체가 MemberCache가 아님: {} (memberId: {})", cached.getClass().getSimpleName(), memberId);
                // 캐시 삭제하고 새로 캐싱
                delete(memberId);
                return Optional.empty();
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            log.debug("캐시에서 멤버 조회 중 오류 발생: {} - {}", memberId, e.getMessage());
            // 오류 발생 시 캐시 삭제
            try {
                delete(memberId);
            } catch (Exception deleteException) {
                log.debug("캐시 삭제 중 오류 발생: {} - {}", memberId, deleteException.getMessage());
            }
            return Optional.empty();
        }
    }

    /**
     * 캐시 삭제 (로그아웃/계정삭제 등)
     */
    public void delete(Long memberId) {
        if (memberId == null) return;
        
        try {
            redis.delete(key(memberId));
            log.debug("멤버 캐시 삭제: {}", memberId);
        } catch (Exception e) {
            log.error("멤버 캐시 삭제 중 오류 발생: {}", memberId, e);
        }
    }

    /**
     * 온라인 상태 확인
     * 캐시에 있으면 online, 없으면 offline
     */
    public boolean isOnline(Long memberId) {
        return get(memberId).isPresent();
    }

    /**
     * 마지막 활동 시간 조회
     * 캐시에서 먼저 확인, 없으면 DB에서 조회
     */
    public Instant lastActiveAt(Long memberId) {
        if (memberId == null) return null;
        
        // 캐시에서 먼저 확인
        Optional<MemberCache> cached = get(memberId);
        if (cached.isPresent()) {
            return cached.get().getLastActiveAt();
        }
        
        // 캐시에 없으면 DB에서 조회
        try {
            Member member = memberRepo.findById(memberId).orElse(null);
            return member != null ? member.getLastActiveAt() : null;
        } catch (Exception e) {
            log.error("DB에서 lastActiveAt 조회 중 오류 발생: {}", memberId, e);
            return null;
        }
    }

    /**
     * 멤버 정보 조회 (캐시 우선, DB fallback)
     */
    public Optional<Member> getMember(Long memberId) {
        if (memberId == null) return Optional.empty();
        
        // 캐시에서 먼저 확인
        Optional<MemberCache> cached = get(memberId);
        if (cached.isPresent()) {
            // 캐시 hit 시 touch로 TTL 연장
            touch(memberId);
            // MemberCache에서 Member로 변환은 복잡하므로 DB에서 조회
        }
        
        // DB에서 조회
        try {
            return memberRepo.findById(memberId);
        } catch (Exception e) {
            log.error("DB에서 멤버 조회 중 오류 발생: {}", memberId, e);
            return Optional.empty();
        }
    }

    /**
     * 모든 멤버 캐시 삭제 (마이그레이션 시 사용)
     */
    public void clearAllCache() {
        try {
            Set<String> keys = redis.keys(PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
                log.info("모든 멤버 캐시 삭제 완료: {} 개", keys.size());
            }
        } catch (Exception e) {
            log.error("캐시 전체 삭제 중 오류 발생", e);
        }
    }

    /**
     * 캐시 마이그레이션 (기존 LinkedHashMap 캐시를 새로운 형식으로 변환)
     */
    public void migrateCache() {
        try {
            Set<String> keys = redis.keys(PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                int migratedCount = 0;
                int deletedCount = 0;
                
                for (String key : keys) {
                    try {
                        Object cached = ops.get(key);
                        if (cached != null && !(cached instanceof MemberCache)) {
                            // LinkedHashMap 등으로 저장된 기존 캐시 삭제
                            redis.delete(key);
                            deletedCount++;
                            log.debug("기존 캐시 삭제: {}", key);
                        } else if (cached instanceof MemberCache) {
                            migratedCount++;
                        }
                    } catch (Exception e) {
                        // 오류 발생 시 해당 키 삭제
                        redis.delete(key);
                        deletedCount++;
                        log.debug("오류로 인한 캐시 삭제: {} - {}", key, e.getMessage());
                    }
                }
                
                log.info("캐시 마이그레이션 완료: 유지 {} 개, 삭제 {} 개", migratedCount, deletedCount);
            }
        } catch (Exception e) {
            log.error("캐시 마이그레이션 중 오류 발생", e);
        }
    }

    /**
     * 캐시 상태 확인
     */
    public boolean isCacheHealthy() {
        try {
            // 간단한 테스트 키로 Redis 연결 확인
            String testKey = "cache_health_test";
            ops.set(testKey, "test", Duration.ofSeconds(10));
            Object result = ops.get(testKey);
            redis.delete(testKey);
            return "test".equals(result);
        } catch (Exception e) {
            log.error("캐시 상태 확인 중 오류 발생", e);
            return false;
        }
    }

    private String key(Long memberId) {
        return PREFIX + memberId;
    }
}