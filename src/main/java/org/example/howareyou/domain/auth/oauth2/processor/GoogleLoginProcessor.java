package org.example.howareyou.domain.auth.oauth2.processor;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.auth.dto.TokenBundle;
import org.example.howareyou.domain.auth.entity.Auth;
import org.example.howareyou.domain.auth.entity.Provider;
import org.example.howareyou.domain.auth.repository.AuthRepository;
import org.example.howareyou.domain.member.redis.MemberCache;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.entity.MemberProfile;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.example.howareyou.domain.member.redis.MemberCacheService;
import org.example.howareyou.global.security.jwt.JwtTokenProvider;
import org.example.howareyou.global.util.UserAgentUtils;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleLoginProcessor implements OAuth2LoginProcessor {

    private final AuthRepository authRepository;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberCacheService memberCacheService;

    @Override
    public Provider provider() {
        return Provider.GOOGLE;
    }

    /**
     * OAuth2 사용자 인증 처리 메서드
     * - 기존 사용자 조회 또는 생성
     * - 토큰 발급 및 캐싱
     */
    @Override
    @Transactional
    public TokenBundle process(OAuth2User oAuth2User, HttpServletRequest request) {
        String providerUserId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

        // 1. 기존 사용자 확인 또는 신규 생성
        Optional<Auth> optionalAuth = authRepository.findByProviderAndProviderUserId(provider(), providerUserId);
        Auth auth = optionalAuth.orElseGet(() -> create(oAuth2User, request));

        // 2. 마지막 로그인 시간 갱신
        auth.updateLastLoginInfo(UserAgentUtils.getClientIP(request));

        // 3. 토큰 발급
        String userId = auth.getMember().getId().toString();
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken();
        LocalDateTime refreshTokenExpiry = LocalDateTime.now().plus(Duration.ofMillis(jwtTokenProvider.getRefreshTokenExpirationTime()));

        // 4. 리프레시 토큰 저장
        auth.setRefreshToken(refreshToken, refreshTokenExpiry);
        authRepository.save(auth);

        // 5. 사용자 정보 캐싱
        cacheUserInfo(auth);

        log.info("Google 로그인 완료: {}", email);
        return new TokenBundle(accessToken, refreshToken, auth.getMember().isProfileCompleted());
    }

    /**
     * 신규 사용자 생성
     */
    @Transactional
    public Auth create(OAuth2User oAuth2User, HttpServletRequest request) {
        String email = oAuth2User.getAttribute("email");
        String providerUserId = oAuth2User.getAttribute("sub");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        // 프로필 생성
        MemberProfile profile = MemberProfile.builder()
                .completed(false)
                .nickname(name)
                .avatarUrl(picture)
                .build();

        // 회원 생성
        Member member = Member.builder()
                .email(email)
                .profile(profile)
                .build();
        profile.setMember(member);
        Member savedMember = memberRepository.save(member);

        // 인증 정보 생성 및 저장
        Auth auth = Auth.builder()
                .provider(provider())
                .providerUserId(providerUserId)
                .email(email)
                .member(savedMember)
                .lastLoginAt(LocalDateTime.now())
                .build();

        return authRepository.save(auth);
    }

    /**
     * Redis에 사용자 정보 캐싱
     */
    private void cacheUserInfo(Auth auth) {
        Member member = auth.getMember();

        MemberCache memberCache = MemberCache.from(member);

        memberCacheService.cacheMember(memberCache);
    }
}