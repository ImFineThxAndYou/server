package org.example.howareyou.global.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.redis.MemberCacheService;
import org.example.howareyou.global.security.CustomMemberDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final MemberCacheService memberCacheService;

    /** JWT 형식인지 간단히 확인 (header.payload.signature) */
    private boolean looksLikeJwt(String token) {
        return token != null && token.chars().filter(ch -> ch == '.').count() == 2;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        // WebSocket 관련 경로는 JWT 필터 스킵
        String requestURI = req.getRequestURI();
        if (requestURI.startsWith("/ws-chatroom/")) {
            chain.doFilter(req, res);
            return;
        }

        String token = null;
        
        // 1. Authorization 헤더에서 토큰 확인
        String bearer = req.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            token = bearer.substring(7).trim();
        }
        
        // 2. 쿼리 파라미터에서 토큰 확인 (SSE 연결용)
        if (token == null) {
            String queryToken = req.getParameter("token");
            if (StringUtils.hasText(queryToken)) {
                token = queryToken.trim();
            }
        }
        
        if (!StringUtils.hasText(token)) {
            chain.doFilter(req, res);      // 토큰 없으면 다음 필터로
            return;
        }

        // 1️⃣ 형식 안 맞으면 바로 패스
        if (!looksLikeJwt(token)) {
            log.debug("Skip non-JWT token: {}", token);
            chain.doFilter(req, res);
            return;
        }

        // 2️⃣ 이미 인증된 상태라면 중복 세팅 방지
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res);
            return;
        }

        try {
            // validate() 내부에서 서명·만료 체크만 하고 subject 반환
            String userId = jwtTokenProvider.validateAndGetSubject(token);

            UserDetails user = userDetailsService.loadUserByUsername(userId); // ← userId=email or memberId
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            /* ④ Presence / TTL 관리 ---------------------------- */
            Long id = ((CustomMemberDetails) user).getId();
            try {
                // 캐시에서 멤버 확인
                if (memberCacheService.get(id).isPresent()) {
                    memberCacheService.touch(id);           // 캐시 hit → TTL 연장 + lastActiveAt 갱신
                } else {
                    memberCacheService.cache(id);           // 캐시 miss → 새로 캐싱
                }
            } catch (Exception e) {
                log.error("멤버 캐싱 처리 중 오류 발생: {}", id, e);
                // 캐싱 오류가 있어도 인증은 계속 진행
            }
            /* -------------------------------------------------- */

            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
        }
        catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
        }

        chain.doFilter(req, res);
    }
}