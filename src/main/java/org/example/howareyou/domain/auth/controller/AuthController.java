package org.example.howareyou.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.auth.dto.TokenBundle;
import org.example.howareyou.domain.auth.service.AuthService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.example.howareyou.global.util.CookieUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "인증", description = "로그인, 로그아웃, 토큰 갱신 관련 API")
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "로그인",
        description = "이메일과 비밀번호로 로그인하여 액세스 토큰과 리프레시 토큰을 발급받습니다. " +
                     "액세스 토큰은 Authorization 헤더로, 리프레시 토큰은 HttpOnly 쿠키로 전송됩니다."
    )
    @PostMapping("/login")
    public ResponseEntity<TokenBundle> login(
            @Parameter(description = "로그인 정보", required = true)
            @RequestBody LoginDto req, 
            HttpServletRequest request, 
            HttpServletResponse response
    ) {
        TokenBundle tokenBundle = authService.login(req.email(), req.password(), request);
        
        // Set tokens in response
        response.setHeader("Authorization", "Bearer " + tokenBundle.access());
        // 개발 환경에서는 secure=false, 프로덕션에서는 secure=true
        boolean isSecure = !"dev".equals(System.getProperty("spring.profiles.active", "dev"));
        response.addCookie(CookieUtils.refresh(tokenBundle.refresh(), isSecure));
        
        // 개발 환경에서는 응답 본문에도 Refresh Token 포함 (HttpOnly 쿠키 읽기 문제 해결)
        return ResponseEntity.ok(tokenBundle);
    }

    @Operation(
        summary = "토큰 갱신",
        description = "리프레시 토큰과 만료된 액세스 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다. " +
                     "리프레시 토큰은 쿠키에서 자동으로 읽어집니다."
    )
    @PostMapping("/refresh")
    public ResponseEntity<TokenBundle> refresh(
            @Parameter(description = "리프레시 토큰 (쿠키에서 자동 읽기)", hidden = true)
            @CookieValue(value = "Refresh", required = false) String refreshToken,
            @Parameter(description = "만료된 액세스 토큰", required = true)
            @RequestHeader("X-Expired-Access-Token") String expiredAccessToken,
            HttpServletRequest request, 
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.AUTH_REFRESH_TOKEN_NOT_FOUND);
        }
        
        TokenBundle tokenBundle = authService.refreshToken(refreshToken, expiredAccessToken);
        response.setHeader("Authorization", "Bearer " + tokenBundle.access());
        
        return ResponseEntity.ok(tokenBundle);
    }

    @Operation(
        summary = "로그아웃",
        description = "현재 세션을 종료하고 리프레시 토큰을 무효화합니다. " +
                     "리프레시 토큰 쿠키도 자동으로 삭제됩니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(description = "리프레시 토큰 (쿠키에서 자동 읽기)", hidden = true)
            @CookieValue(value = "Refresh", required = false) String refreshToken, 
            HttpServletResponse response
    ) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        
        // Clear the refresh token cookie
        response.addCookie(CookieUtils.expire());
        
        return ResponseEntity.ok().build();
    }

    /* 내부 DTO */
    public record LoginDto(String email,String password){}
}