package org.example.howareyou.domain.auth.dto;

/** 
 * 인증 토큰 번들
 * @param access 액세스 토큰
 * @param refresh 리프레시 토큰  
 * @param completed 프로필 완료 여부 (membername + 필수 프로필 정보 모두 완료)
 */
public record TokenBundle(
        String access,
        String refresh,
        boolean completed
){}