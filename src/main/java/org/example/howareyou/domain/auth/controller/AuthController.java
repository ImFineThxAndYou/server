package org.example.howareyou.domain.auth.controller;

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
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenBundle> login(@RequestBody LoginDto req, HttpServletRequest request, HttpServletResponse response){
        TokenBundle tokenBundle = authService.login(req.email(), req.password(), request);
        
        // Set tokens in response
        response.setHeader("Authorization", "Bearer " + tokenBundle.access());
        response.addCookie(CookieUtils.refresh(tokenBundle.refresh(), false));
        
        // 개발 환경에서는 응답 본문에도 Refresh Token 포함 (HttpOnly 쿠키 읽기 문제 해결)
        return ResponseEntity.ok(tokenBundle);
    }
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(@CookieValue(value = "Refresh", required = false) String refreshToken, HttpServletRequest request, HttpServletResponse response){
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.AUTH_REFRESH_TOKEN_NOT_FOUND);
        }
        
        String newAccessToken = authService.refresh(refreshToken);
        response.setHeader("Authorization", "Bearer " + newAccessToken);
        
        return ResponseEntity.ok().build();
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = "Refresh", required = false) String refreshToken, HttpServletResponse response){
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