package org.example.howareyou.domain.recommendationtag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.entity.MemberTag;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.example.howareyou.domain.recommendationtag.entity.MemberTagScore;
import org.example.howareyou.domain.recommendationtag.repository.MemberTagScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 기존 사용자들의 MemberTagScore를 생성하는 마이그레이션 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberTagScoreMigrationService {
    
    private final MemberRepository memberRepository;
    private final MemberTagScoreRepository memberTagScoreRepository;
    private final RecommendationTagService recommendationTagService;
    
    /**
     * 모든 기존 사용자에 대해 MemberTagScore 생성/마이그레이션
     */
    @Transactional
    public MigrationResult migrateAllUsers() {
        log.info("🚀 전체 사용자 MemberTagScore 마이그레이션 시작");
        
        List<Member> allMembers = memberRepository.findAll();
        int totalUsers = allMembers.size();
        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;
        
        for (Member member : allMembers) {
            try {
                if (migrateUser(member)) {
                    successCount++;
                } else {
                    skipCount++;
                }
            } catch (Exception e) {
                log.error("사용자 마이그레이션 실패: memberId={}, error={}", member.getId(), e.getMessage(), e);
                errorCount++;
            }
        }
        
        MigrationResult result = new MigrationResult(totalUsers, successCount, skipCount, errorCount);
        
        log.info("✅ 마이그레이션 완료: {}", result);
        return result;
    }
    
    /**
     * 특정 사용자에 대해 MemberTagScore 생성/마이그레이션
     */
    @Transactional
    public boolean migrateUser(Member member) {
        Long memberId = member.getId();
        
        // 이미 MemberTagScore가 있는지 확인
        List<MemberTagScore> existingScores = memberTagScoreRepository.findByMemberId(memberId);
        if (!existingScores.isEmpty()) {
            log.debug("사용자 {}는 이미 MemberTagScore가 존재합니다. 건너뜁니다.", memberId);
            return false;
        }
        
        // 프로필에서 관심사 가져오기
        Set<MemberTag> interests = member.getProfile() != null ? 
                member.getProfile().getInterests() : Set.of();
        
        if (interests.isEmpty()) {
            log.debug("사용자 {}의 관심사가 설정되지 않았습니다. 기본 태그로 초기화합니다.", memberId);
            // 기본 태그들로 초기화 (가장 일반적인 관심사들)
            interests = Set.of(
                    MemberTag.TECHNOLOGY,
                    MemberTag.MUSIC,
                    MemberTag.FOOD,
                    MemberTag.TRAVEL
            );
        }
        
        // MemberTagScore 생성
        recommendationTagService.createOrUpdateMemberTagScores(memberId, interests);
        
        log.info("사용자 {} 마이그레이션 완료: {}개 관심사", memberId, interests.size());
        return true;
    }
    
    /**
     * 특정 사용자 ID로 마이그레이션
     */
    @Transactional
    public boolean migrateUserById(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + memberId));
        
        return migrateUser(member);
    }
    
    /**
     * 마이그레이션 결과 DTO
     */
    public static class MigrationResult {
        private final int totalUsers;
        private final int successCount;
        private final int skipCount;
        private final int errorCount;
        
        public MigrationResult(int totalUsers, int successCount, int skipCount, int errorCount) {
            this.totalUsers = totalUsers;
            this.successCount = successCount;
            this.skipCount = skipCount;
            this.errorCount = errorCount;
        }
        
        // Getters
        public int getTotalUsers() { return totalUsers; }
        public int getSuccessCount() { return successCount; }
        public int getSkipCount() { return skipCount; }
        public int getErrorCount() { return errorCount; }
        
        @Override
        public String toString() {
            return String.format("전체: %d, 성공: %d, 건너뜀: %d, 실패: %d", 
                    totalUsers, successCount, skipCount, errorCount);
        }
    }
}
