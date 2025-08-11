package org.example.howareyou.domain.auth.dto;

/** access / refresh / 프로필완료 플래그 */
public record TokenBundle(
        String access,
        String refresh,
        boolean completed
){}