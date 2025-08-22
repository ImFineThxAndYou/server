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

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    /* ──────────── Dependencies ──────────── */
    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2SuccessHandler successHandler;
    private final Environment environment;

    /* ──────────── CORS Origins (prod에서 사용) ────────────
       application.yml (또는 ENV) 예:
       front:
         cors:
           allowed-origins:
             - "https://your-prod-frontend.com"
             - "https://another-allowed-site.com"
    */
    @Value("${front.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    /* ──────────── Security Filter Chain ──────────── */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // 프리플라이트(OPTIONS) 전역 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Swagger & 정적 리소스
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()

                        // k6 테스트
                    .requestMatchers(
                        "/api/chat-message/**",
                        "/analyze/**"

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
                                "/actuator/**",
                                "/upload-csv"
                        } : new String[]{}).permitAll()

                        // 읽기 전용 공개 API
                        .requestMatchers(HttpMethod.GET,
                                "/api/members/*",
                                "/api/members/*/status",
                                "/api/members/membername/*"
                        ).permitAll()

                        // SSE 는 인증 필요
                        .requestMatchers("/api/notify/sse").authenticated()

                        // 그 외는 인증
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 -> oauth2.successHandler(successHandler))

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // 개발(h2-console 등 frame 허용)
        if (isDevProfile()) {
            http.headers(headers -> headers.frameOptions().disable());
            log.info("🔧 개발 환경: 무제한 CORS / dev 허용 경로 / H2 콘솔 프레임 허용");
        } else {
            log.info("🚀 운영 환경: 제한된 CORS (화이트리스트 기반)");
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

        if (isDevProfile()) {
            // ✅ 개발: 모든 Origin/메서드/헤더 허용
            cfg.addAllowedOriginPattern("*");
            cfg.addAllowedMethod(CorsConfiguration.ALL);
            cfg.addAllowedHeader(CorsConfiguration.ALL);
            cfg.setAllowCredentials(true);
            // 노출할 헤더
            cfg.addExposedHeader("Authorization");
        } else {
            // ✅ 운영: 화이트리스트 기반
            // allowCredentials(true)일 때는 addAllowedOriginPattern("*") 사용 불가
            cfg.setAllowedOrigins(allowedOrigins);
            cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
            cfg.setAllowedHeaders(List.of("*"));
            cfg.setAllowCredentials(true);
            cfg.setExposedHeaders(List.of("Authorization"));
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
        return profiles.length == 0 || Arrays.asList(profiles).contains("dev");
    }
}