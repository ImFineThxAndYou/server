package org.example.howareyou.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.howareyou.global.entity.BaseEntity;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * 서비스 내 ‘회원’ 루트 엔티티
 * ──────────────────────────────────────────────────────
 * • 이메일(로그인 ID) · 활성 플래그 · 마지막 로그인 시각
 * • MemberProfile 1 : 1 관계 (cascade + orphanRemoval)
 */
@Entity
@Table(
        name = "members",
        indexes = @Index(name = "idx_member_email", columnList = "email", unique = true)
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Member extends BaseEntity {


    /* ==================== 기본 정보 ==================== */

    @Column(nullable = false, unique = true, length = 100)
    private String email;                // 로그인 이메일

    @Column(length = 30, unique = true)
    private String membername;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;       // 활성화 여부


    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;   // 마지막 로그인 시간

    /* ==================== 프로필 연관 ==================== */

    @OneToOne(
            mappedBy = "member",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    private MemberProfile profile;

    /* ==================== 정적 생성 ==================== */

    public static Member create(String email) {
        if (!StringUtils.hasText(email))
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "이메일은 필수입니다.");
        return Member.builder()
                .email(email.trim().toLowerCase())
                .build();
    }

    /* ==================== 연관 편의 ==================== */

    public void setProfile(MemberProfile profile) {
        if (this.profile != null) this.profile.setMember(null);
        this.profile = profile;
        if (profile != null && profile.getMember() != this)
            profile.setMember(this);
    }

    /* ==================== 비즈니스 로직 ==================== */

    /**
     * 프로필 생성·업데이트 (필수: nickname)
     * null 파라미터 → ‘변경 없음’
     */
    public Member updateOrCreateProfile(
            String nickname,
            String avatarUrl,
            String statusMessage,
            Set<Category> interests,
            LocalDate birthDate,
            String country,
            String region,
            String language,
            String timezone
    ) {
        if (!StringUtils.hasText(nickname))
            throw new CustomException(ErrorCode.INVALID_NICKNAME);

        if (this.profile == null) {                     // 신규 생성
            this.profile = MemberProfile.create(
                    nickname, avatarUrl, statusMessage, interests);
            this.profile.setMember(this);
            // 추가 필드는 별도 반영
            this.profile.updateProfile(
                    nickname, statusMessage, avatarUrl, interests,
                    birthDate, country, region, language, timezone);
        } else {                                        // 기존 업데이트
            this.profile.updateProfile(
                    nickname, statusMessage, avatarUrl, interests,
                    birthDate, country, region, language, timezone);
        }
        return this;
    }

    /** 프로필 작성 완료 */
    public void completeProfile() {
        if (this.profile != null) this.profile.completeProfile();
    }

    /** 프로필 완료 여부 */
    public boolean isProfileCompleted() {
        return this.profile != null && this.profile.isCompleted();
    }

    /** 계정 삭제(soft-delete) */
    public void deleteAccount() {
        this.active = false;
        this.email = "deleted_" + this.getId() + "_" + this.email;
        if (this.profile != null) this.profile.clearPersonalInfo();
    }

    /** 마지막 로그인 시각 갱신 */
    public void updateLastLogin() { this.lastLoginAt = LocalDateTime.now(); }

    /* ==================== equals & hashCode ==================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemberProfile mp)) return false;
        return getId() != null && getId().equals(mp.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}