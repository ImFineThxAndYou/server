package org.example.howareyou.domain.member.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.example.howareyou.global.entity.BaseEntity;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;

/**
 * 통합 회원 프로필 엔티티
 * ──────────────────────────────────────────────────────
 * • 닉네임 / 아바타 / 상태메시지 / 관심사(Set<MemberTag>)
 * • 언어·시간대(기존 MemberSettings 통합)
 * • 생년월일 → 나이 계산 (getAge())
 * • 거주지 : country(ISO-3166 alpha-2) + region(주·도·시 등)
 */
@Entity
@Table(name = "member_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MemberProfile extends BaseEntity {

    @JsonIgnore                       // 역직렬화 순환 예방
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "member_id")
    private Member member;

    /* ==================== 프로필 기본 ==================== */

    @Column(nullable = false, length = 50)
    private String nickname;                          // 닉네임 (필수)

    @Column(length = 500)
    private String avatarUrl;                         // 프로필 이미지

    @Column(length = 100)
    private String bio;                     // 상태 메시지

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "member_interests",
            joinColumns = @JoinColumn(name = "member_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "interest")
    @Builder.Default
    private Set<MemberTag> interests = new HashSet<>();// 관심사

    /* ==================== 라이프스타일 & 로케일 ==================== */

    @Column(nullable = false)
    @Builder.Default
    private boolean completed = false;                // 프로필 완료 여부

    @Column(nullable = false, length = 2)
    @Builder.Default
    private String language = "ko";                   // 언어 (ko / en …)

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String timezone = "Asia/Seoul";           // 시간대

    /* ==================== 추가 정보 ==================== */

    private LocalDate birthDate;                      // 생년월일

    @Column(length = 2)
    private String country;                           // 국가코드 (ISO-3166)

    @Column(length = 50)
    private String region;                            // 주·도·시 등

    /* ==================== 생성 메서드 ==================== */

    public static MemberProfile create(
            String nickname,
            String avatarUrl,
            String bio,
            Set<MemberTag> interests
    ) {
        return MemberProfile.builder()
                .nickname(nickname)
                .avatarUrl(avatarUrl)
                .bio(bio)
                .interests(interests != null ? interests : new HashSet<>())
                .completed(false)
                .build();
    }

    /* ==================== 연관 편의 ==================== */

    public void setMember(Member member) {
        if (this.member != null) {
            this.member.setProfile(null);
        }
        this.member = member;
        if (member != null && member.getProfile() != this) {
            member.setProfile(this);
        }
    }

    /* ==================== 비즈니스 로직 ==================== */

    /**
     * 프로필 정보 업데이트
     * null  → “변경 없음” 으로 해석
     */
    public void updateProfile(
            String nickname,
            String bio,
            String avatarUrl,
            Set<MemberTag> interests,
            LocalDate birthDate,
            String country,
            String region,
            String language,
            String timezone
    ) {
        /* ① 필수값 검증 */
        if (!StringUtils.hasText(nickname))
            throw new CustomException(ErrorCode.INVALID_NICKNAME);
        this.nickname = nickname.trim();

        /* ② 선택 필드 */
        if (bio         != null) this.bio = bio;
        if (avatarUrl   != null) this.avatarUrl     = avatarUrl;
        if (birthDate   != null) this.birthDate     = birthDate;
        if (country     != null) this.country       = country.toUpperCase(Locale.ENGLISH);
        if (region      != null) this.region        = region;

        /* ③ 관심사 (null = 유지, 빈 Set = 전체삭제) */
        if (interests != null) {
            this.interests.clear();
            this.interests.addAll(interests);
        }

        /* ④ 로케일 */
        if (language != null) setLanguage(language);
        if (timezone != null) setTimezone(timezone);
    }

    /** 프로필 작성 완료 플래그 - 최초 1회만 호출 */
    public void completeProfile() { this.completed = true; }

    /** 현재 나이 (birthDate가 없으면 -1) */
    public int getAge() {
        return birthDate == null ? -1 :
                Period.between(birthDate, LocalDate.now()).getYears();
    }

    /** 개인정보 비식별화 (계정 삭제 시 호출) */
    public void clearPersonalInfo() {
        this.nickname = "deleted_" + this.getId();
        this.avatarUrl = null;
        this.bio = null;
        this.interests.clear();
        this.birthDate = null;
        this.country = null;
        this.region  = null;
    }

    /* ==================== 로케일 유효성 ==================== */

    public void setLanguage(String language) {
        this.language = switch (Optional.ofNullable(language).orElse("ko").toLowerCase())
                        {
                            case "en" -> "en";
                            default   -> "ko";
                        };
    }

    public void setTimezone(String timezone) {
        try { ZoneId.of(timezone); this.timezone = timezone; }
        catch (Exception e) { this.timezone = "Asia/Seoul"; }
    }

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