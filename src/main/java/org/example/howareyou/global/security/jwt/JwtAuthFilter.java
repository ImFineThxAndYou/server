package org.example.howareyou.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;       // JWT 토큰 유틸 클래스 (파싱, 생성 등)
    private final UserDetailsService uds;     // 사용자 정보를 로드하기 위한 서비스

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String bearer = req.getHeader("Authorization"); // Authorization 헤더에서 토큰 추출
        if (bearer != null && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7); // "Bearer " 이후의 실제 토큰만 추출
            try {
                Claims c = jwt.parse(token); // 토큰을 파싱해서 Claims (payload) 추출
                UserDetails u = uds.loadUserByUsername(c.getSubject()); // 토큰의 subject로 사용자 조회 (이거 username으로 가져오는거 일단 mail로 할지 확인해봐야함)

                // 인증 객체를 생성해서 SecurityContext에 설정
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities())
                );
            } catch(ExpiredJwtException e){ throw new CustomException(ErrorCode.AUTH_TOKEN_EXPIRED);}
        }

        chain.doFilter(req, res); // 다음 필터 또는 서블릿으로 요청 전달
    }
}