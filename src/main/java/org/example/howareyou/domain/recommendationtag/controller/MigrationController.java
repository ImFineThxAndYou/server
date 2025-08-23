package org.example.howareyou.domain.recommendationtag.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.recommendationtag.service.MemberTagScoreMigrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * MemberTagScore 마이그레이션을 위한 컨트롤러
 * 개발/운영 환경에서만 사용
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/migration")
@Tag(name = "마이그레이션", description = "MemberTagScore 마이그레이션 관리")
public class MigrationController {
    
    private final MemberTagScoreMigrationService migrationService;
    
    @Operation(
            summary = "전체 사용자 MemberTagScore 마이그레이션",
            description = "모든 기존 사용자에 대해 MemberTagScore를 생성합니다."
    )
    @PostMapping("/member-tag-scores")
    @PreAuthorize("hasRole('ADMIN')") // 관리자만 접근 가능
    public ResponseEntity<MemberTagScoreMigrationService.MigrationResult> migrateAllUsers() {
        log.info("🔧 전체 사용자 MemberTagScore 마이그레이션 요청");
        
        try {
            MemberTagScoreMigrationService.MigrationResult result = migrationService.migrateAllUsers();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("마이그레이션 실행 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @Operation(
            summary = "특정 사용자 MemberTagScore 마이그레이션",
            description = "지정된 사용자 ID에 대해 MemberTagScore를 생성합니다."
    )
    @PostMapping("/member-tag-scores/{memberId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> migrateUser(@PathVariable Long memberId) {
        log.info("🔧 사용자 {} MemberTagScore 마이그레이션 요청", memberId);
        
        try {
            boolean success = migrationService.migrateUserById(memberId);
            if (success) {
                return ResponseEntity.ok("사용자 " + memberId + " 마이그레이션 완료");
            } else {
                return ResponseEntity.ok("사용자 " + memberId + "는 이미 마이그레이션되었습니다");
            }
        } catch (Exception e) {
            log.error("사용자 {} 마이그레이션 중 오류 발생", memberId, e);
            return ResponseEntity.internalServerError()
                    .body("마이그레이션 실패: " + e.getMessage());
        }
    }
    
    @Operation(
            summary = "마이그레이션 상태 확인",
            description = "현재 마이그레이션 상태를 확인합니다."
    )
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getMigrationStatus() {
        return ResponseEntity.ok("마이그레이션 서비스가 정상적으로 실행 중입니다.");
    }
}
