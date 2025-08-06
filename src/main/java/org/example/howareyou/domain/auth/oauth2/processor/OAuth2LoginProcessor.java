package org.example.howareyou.domain.auth.oauth2.processor;

import jakarta.servlet.http.HttpServletRequest;
import org.example.howareyou.domain.auth.dto.TokenBundle;
import org.example.howareyou.domain.auth.entity.Provider;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * OAuth2 로그인 처리를 위한 프로세서 인터페이스
 */
public interface OAuth2LoginProcessor {
    
    /**
     * 이 프로세서가 처리할 OAuth2 공급자를 반환합니다.
     *
     * @return OAuth2 공급자
     */
    Provider provider();
    
    /**
     * OAuth2 인증 처리를 수행합니다.
     *
     * @param oAuth2User OAuth2 사용자 정보
     * @param request HTTP 요청 객체
     * @return 인증 토큰 번들 (액세스 토큰, 리프레시 토큰 포함)
     */
    TokenBundle process(OAuth2User oAuth2User, HttpServletRequest request);
}