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
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
        return allowedOriginsRaw == null ? List.of()
                : allowedOriginsRaw.stream().map(String::trim)
                .filter(s -> !s.isBlank()).distinct().collect(Collectors.toList());
    }

    /* ──────────── Security Filter Chain ──────────── */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
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

                        // k6 테스트
                    .requestMatchers(
                        "/api/chat-message/**",
                        "/analyze/**",
                        "/ws-chatroom"


                    ).permitAll()

                        // 인증/회원 관련 공개 API
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/signup/**").permitAll()



                    // WebSocket 관련 경로 허용 (SockJS info, sockjs-node 등)
                        .requestMatchers("/ws-chatroom/**").permitAll()
                        .requestMatchers("/ws-chatroom/**", "/topic/**", "/app/**").permitAll()


                    // 개발 환경 전용 허용 경로
                        .requestMatchers(isDevProfile() ? new String[]{
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
                                "/actuator/**",     // dev에선 actuator 전체 열어도 됨
                                "/upload-csv"
                        } : new String[]{}).permitAll()

                        // 공개 GET 조회
                        .requestMatchers(HttpMethod.GET,
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

        // 개발: H2 console 프레임 허용
        if (isDevProfile()) {
            http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
            log.info("🔧 dev profile: wide-open CORS + dev routes permitted + H2 frame allowed");
        } else {
            log.info("🚀 prod profile: CORS whitelist + auth-by-default");
        }

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

        if (isDevProfile()) {
            // 개발: 어디서든 붙을 수 있게
            cfg.addAllowedOriginPattern("*");  // credentials true + pattern 허용
        } else {
            // 운영: 화이트리스트만
            List<String> origins = allowedOrigins();
            if (origins.isEmpty()) {
                // 운영인데 비어있으면 실수 방지용으로 로그만 남기고 막음(필요시 기본값 추가)
                log.warn("CORS allowed-origins is empty on PROD. Check your config/env!");
            }
            cfg.setAllowedOrigins(origins);     // credentials true + exact origin만
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
        // 프로필 미설정 시 dev로 간주(로컬 기본값)
        return profiles.length == 0 || Arrays.asList(profiles).contains("dev");
    }
}