package org.example.howareyou.global.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.auth.oauth2.handler.OAuth2SuccessHandler;
import org.example.howareyou.global.security.jwt.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    /* ──────────── Dependencies ──────────── */
    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2SuccessHandler successHandler;
    private final Environment environment;

    /*
     * 운영 CORS 화이트리스트 (콤마 구분 env 지원 + yml 리스트 둘 다 지원)
     * 예) FRONT_CORS_ALLOWED_ORIGINS="https://howareu.click,https://www.howareu.click"
     * 또는 application-prod.yml:
     * front:
     *   cors:
     *     allowed-origins:
     *       - https://howareu.click
     *       - https://www.howareu.click
     */
    @Value("#{'${front.cors.allowed-origins:https://howareu.click,https://www.howareu.click}'.split(',')}")
    private List<String> allowedOriginsRaw;

    private List<String> allowedOrigins() {
        List<String> origins = allowedOriginsRaw == null ? List.of()
                : allowedOriginsRaw.stream().map(String::trim)
                .filter(s -> !s.isBlank()).distinct().collect(Collectors.toList());
        
        log.info("🔒 CORS allowed origins: {}", origins);
        return origins;
    }

    /* ──────────── Security Filter Chain ──────────── */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // REST API라 CSRF 미사용. (필요 시 특정 경로만 ignore)
                .csrf(csrf -> csrf.disable())

                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 없이 동작(JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 인가 규칙
                .authorizeHttpRequests(auth -> auth
                        // 프리플라이트(OPTIONS) 전체 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 헬스체크/기본 공개 엔드포인트
                        .requestMatchers("/", "/favicon.ico", "/robots.txt").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // Swagger & 정적 리소스
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/error"
                        ).permitAll()

                        // 인증/회원 공개 API
                        .requestMatchers("/api/auth/**").permitAll()

                        // WebSocket 핸드셰이크 경로 (SockJS info 포함) 공개
                        .requestMatchers(
                                "/ws-chatroom/**",
                                "/ws/**",
                                "/sockjs-node/**"
                        ).permitAll()

                                // 개발/프로덕션 공통 허용 경로 (모든 환경에서 적용)
        .requestMatchers(new String[]{
                "/",
                "/index.html",
                "/notification-test.html",
                "/test-login.html",
                "/test-signup.html",
                "/test-info.html",
                "/js/**",
                "/api/test/**",
                "/test/**",
                "/dev/**",
                "/debug/**",
                "/h2-console/**",
                "/actuator/**",     // 모든 환경에서 actuator 허용
                "/upload-csv"
        }).permitAll()

                        // 공개 GET 조회
                        .requestMatchers(HttpMethod.GET,
                                "/api/members/*",
                                "/api/members/*/status",
                                "/api/members/membername/*"
                        ).permitAll()
                        //
                        .requestMatchers(HttpMethod.POST,
                                "/api/members/*",
                                "/api/members/*/status",
                                "/api/members/membername/*"
                        ).permitAll()
                        //server health 체크
                        .requestMatchers("/health").permitAll()

                        // SSE(알림)는 인증 필요
                        .requestMatchers("/api/notify/sse").authenticated()

                        // 이외 전부 인증
                        .anyRequest().authenticated()
                )

                // OAuth2 로그인 (성공 시 커스텀 핸들러)
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(successHandler)
                )

                // 예외 처리 - JSON 응답
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )

                // JWT 필터 위치 조정
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // 모든 환경에서 동일한 설정 적용
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        log.info("🔧 All profiles: wide-open CORS + dev routes permitted + H2 frame allowed");

        return http.build();
    }

    /* ──────────── Beans ──────────── */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (req, res, ex) -> jsonError(res, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (req, res, ex) -> jsonError(res, HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
    }

    /* ──────────── CORS ──────────── */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // 공통
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        // 토큰/쿠키 노출 필요시
        cfg.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        // 모든 환경에서 CORS 허용 (dev와 prod 동일하게)
        List<String> origins = allowedOrigins();
        if (origins.isEmpty()) {
            // CORS 설정이 비어있으면 모든 origin 허용 (개발 편의)
            log.warn("CORS allowed-origins is empty. Allowing all origins for development convenience.");
            cfg.addAllowedOriginPattern("*");
        } else {
            // 설정된 origins만 허용
            cfg.setAllowedOrigins(origins);
        }

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    /* ──────────── Util ──────────── */
    private void jsonError(HttpServletResponse res, HttpStatus status, String msg) throws java.io.IOException {
        log.warn("[SECURITY] {}: {}", status, msg);
        res.setStatus(status.value());
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"message\": \"" + msg + "\"}");
    }

    private boolean isDevProfile() {
        String[] profiles = environment.getActiveProfiles();
        boolean isDev = profiles.length == 0 || Arrays.asList(profiles).contains("dev");
        log.info("🔧 Active profiles: {}, isDev: {}", Arrays.toString(profiles), isDev);
        return isDev;
    }
}