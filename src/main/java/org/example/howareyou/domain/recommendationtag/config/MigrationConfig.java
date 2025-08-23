package org.example.howareyou.domain.recommendationtag.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.recommendationtag.service.MemberTagScoreMigrationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * 애플리케이션 시작 시 자동 마이그레이션 설정
 * application.yml에서 migration.auto.enabled=true로 설정하면 활성화
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "migration.auto.enabled", havingValue = "true")
public class MigrationConfig {
    
    private final MemberTagScoreMigrationService migrationService;
    
    /**
     * 애플리케이션 시작 완료 후 자동 마이그레이션 실행
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationStart() {
        log.info("🚀 애플리케이션 시작 완료. 자동 마이그레이션을 시작합니다...");
        
        try {
            var result = migrationService.migrateAllUsers();
            log.info("✅ 자동 마이그레이션 완료: {}", result);
        } catch (Exception e) {
            log.error("❌ 자동 마이그레이션 실패", e);
        }
    }
}
