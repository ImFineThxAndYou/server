package org.example.howareyou.global.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import lombok.Setter;

/**
 * 모든 엔티티의 공통 필드 + 수명주기 콜백으로 타임스탬프 관리
 * createdAt : 최초 INSERT 시 한 번만 세팅
 * updatedAt : INSERT · UPDATE 마다 변경
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    /* ------------ 공통 PK (UUID) ------------ */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false)
    private Instant createdAt;       // 최초 생성 시각 (UTC)

    private Instant updatedAt;       // 마지막 수정 시각 (UTC)

    /** INSERT 직전에 호출 */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** UPDATE 직전에 호출 */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}