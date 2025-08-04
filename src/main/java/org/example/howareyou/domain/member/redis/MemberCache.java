package org.example.howareyou.domain.member.redis;

import lombok.Builder;
import lombok.Getter;
import org.example.howareyou.domain.member.entity.Member;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class MemberCache implements Serializable {
    private static final long serialVersionUID = 1L;

    private String          id;
    private String        email;
    private String        nickname;
    private String        avatarUrl;
    private boolean       online;
    private LocalDateTime lastActiveAt;
    private boolean       profileCompleted;

    /* ---------- Factory ---------- */
    public static MemberCache from(Member m) {
        if (m == null) return null;

        return MemberCache.builder()
                .id(String.valueOf(m.getId()))               // ← long → String
                .email(m.getEmail())
                .nickname(m.getProfile() != null ? m.getProfile().getNickname() : null)
                .avatarUrl(m.getProfile() != null ? m.getProfile().getAvatarUrl() : null)
                .online(true)
                .lastActiveAt(LocalDateTime.now())
                .profileCompleted(m.isProfileCompleted())
                .build();
    }

    /* ---------- Mutators ---------- */
    /** 마지막 활동 시각을 NOW 로 갱신하고 online=true */
    public MemberCache updateAsActive() {
        return this.toBuilder()
                .online(true)
                .lastActiveAt(LocalDateTime.now())
                .build();
    }

    /** online=false 로 전환(로그아웃 등) */
    public MemberCache updateAsInactive() {
        return this.toBuilder()
                .online(false)
                .build();
    }

    /* Lombok toBuilder 사용 */
    private MemberCacheBuilder toBuilder() {
        return MemberCache.builder()
                .id(id).email(email)
                .nickname(nickname).avatarUrl(avatarUrl)
                .online(online).lastActiveAt(lastActiveAt)
                .profileCompleted(profileCompleted);
    }
}