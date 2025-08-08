package org.example.howareyou.global.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.auth.entity.Auth;
import org.example.howareyou.domain.auth.entity.Provider;
import org.example.howareyou.domain.auth.repository.AuthRepository;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.entity.MemberProfile;
import org.example.howareyou.domain.member.entity.Role;
import org.example.howareyou.domain.member.redis.MemberCacheService;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 개발 환경에서 사용하는 테스트용 컨트롤러
 * - 테스트 계정 생성
 * - 캐시 관리
 * - 데이터베이스 상태 확인
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class TestController {

    private final MemberRepository memberRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberCacheService memberCacheService;

    /**
     * 테스트 계정 생성
     */
    @PostMapping("/create-account")
    public ResponseEntity<Map<String, Object>> createTestAccount(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            String email = request.get("email");
            String password = request.get("password");
            String membername = request.get("membername");

            if (email == null || password == null || membername == null) {
                result.put("success", false);
                result.put("message", "email, password, membername이 모두 필요합니다.");
                return ResponseEntity.badRequest().body(result);
            }

            // 중복 체크
            if (memberRepository.existsByMembername(membername)) {
                result.put("success", false);
                result.put("message", "이미 존재하는 membername입니다: " + membername);
                return ResponseEntity.badRequest().body(result);
            }

            // Member 생성
            Member member = Member.builder()
                    .email(email)
                    .membername(membername)
                    .role(Role.valueOf("USER"))
                    .active(true)
                    .build();

            memberRepository.save(member);

            // MemberProfile 생성
            MemberProfile profile = MemberProfile.builder()
                    .member(member)
                    .nickname(membername)
                    .language("ko")
                    .timezone("Asia/Seoul")
                    .completed(false)
                    .interests(Set.of())
                    .build();

            member.setProfile(profile);
            memberRepository.save(member);

            // Auth 생성
            Auth auth = Auth.builder()
                    .member(member)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(password))
                    .provider(Provider.valueOf("LOCAL"))
                    .build();

            authRepository.save(auth);

            result.put("success", true);
            result.put("message", "테스트 계정이 생성되었습니다.");
            result.put("memberId", member.getId());
            result.put("email", email);
            result.put("membername", membername);

            log.info("테스트 계정 생성: {} ({})", email, membername);

        } catch (Exception e) {
            log.error("테스트 계정 생성 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "계정 생성 중 오류 발생: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 기본 테스트 계정들 생성
     */
    @PostMapping("/create-default-accounts")
    public ResponseEntity<Map<String, Object>> createDefaultAccounts() {
        Map<String, Object> result = new HashMap<>();

        try {
            createTestAccount(Map.of(
                    "email", "test@example.com",
                    "password", "password123!",
                    "membername", "testuser"
            ));

            createTestAccount(Map.of(
                    "email", "admin@example.com",
                    "password", "admin123!",
                    "membername", "admin"
            ));

            createTestAccount(Map.of(
                    "email", "user1@example.com",
                    "password", "user123!",
                    "membername", "user1"
            ));

            result.put("success", true);
            result.put("message", "기본 테스트 계정들이 생성되었습니다.");

        } catch (Exception e) {
            log.error("기본 테스트 계정 생성 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "기본 계정 생성 중 오류 발생: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 데이터베이스 상태 확인
     */
    @GetMapping("/db-status")
    public ResponseEntity<Map<String, Object>> checkDatabaseStatus() {
        Map<String, Object> result = new HashMap<>();

        try {
            long memberCount = memberRepository.count();
            long authCount = authRepository.count();

            result.put("success", true);
            result.put("memberCount", memberCount);
            result.put("authCount", authCount);
            result.put("message", "데이터베이스 상태 정상");

        } catch (Exception e) {
            log.error("데이터베이스 상태 확인 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "데이터베이스 상태 확인 중 오류 발생: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 모든 캐시 삭제
     */
    @PostMapping("/clear-cache")
    public ResponseEntity<Map<String, Object>> clearCache() {
        Map<String, Object> response = new HashMap<>();
        try {
            memberCacheService.clearAllCache();
            response.put("success", true);
            response.put("message", "모든 멤버 캐시가 삭제되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "캐시 삭제 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 캐시 마이그레이션
     */
    @PostMapping("/migrate-cache")
    public ResponseEntity<Map<String, Object>> migrateCache() {
        Map<String, Object> response = new HashMap<>();
        try {
            memberCacheService.migrateCache();
            response.put("success", true);
            response.put("message", "캐시 마이그레이션이 완료되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "캐시 마이그레이션 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 캐시 상태 확인
     */
    @GetMapping("/cache-health")
    public ResponseEntity<Map<String, Object>> checkCacheHealth() {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean isHealthy = memberCacheService.isCacheHealthy();
            response.put("success", true);
            response.put("cacheHealthy", isHealthy);
            response.put("message", isHealthy ? "캐시 상태 정상" : "캐시 상태 이상");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "캐시 상태 확인 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}