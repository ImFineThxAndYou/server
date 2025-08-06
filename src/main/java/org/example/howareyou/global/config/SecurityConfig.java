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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
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

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {



    /* ──────────── Dependencies ──────────── */
    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2SuccessHandler successHandler;
    private final Environment environment;

    /* ──────────── CORS Origins (@Value 로 yml 주입 가능) ──────────── */
    @Value("${front.cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

    /* ──────────── Security Filter Chain ──────────── */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Swagger 허용
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // 인증/회원 관련 공개 API
                        .requestMatchers("/api/auth/**").permitAll()

                        // 개발 환경에서만 테스트 및 개발 도구 허용
                        .requestMatchers(isDevProfile() ? new String[]{
                                "/test-login.html",
                                "/test-signup.html",
                                "/test-info.html",
                                "/api/test/**",
                                "/test/**",
                                "/dev/**",
                                "/debug/**",
                                "/h2-console/**",
                                "/actuator/**",
                                "/error",
                                "/favicon.ico"
                        } : new String[]{}).permitAll()

                        // 읽기 전용 API (공개)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/members/*",
                                "/api/v1/members/*/status",
                                "/api/v1/members/membername/*"
                        ).permitAll()

                        // ✅ CSV 업로드 API 허용
                        .requestMatchers("/upload-csv").permitAll()

                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(successHandler)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // 개발 환경에서 H2 콘솔 프레임 허용
        if (isDevProfile()) {
            http.headers(headers -> headers.frameOptions().disable());
            log.info("🔧 개발 환경: 테스트 및 개발 도구 경로가 활성화되었습니다.");
        } else {
            log.info("🚀 프로덕션 환경: 보안 강화 모드로 실행됩니다.");
        }

        return http.build();
    }



    /* ──────────── Beans ──────────── */
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

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
        cfg.setAllowedOrigins(allowedOrigins);   // yml 또는 ENV 로 관리
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setExposedHeaders(List.of("Authorization"));

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

    /**
     * 개발 환경인지 확인
     */
    private boolean isDevProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length == 0 ||
               java.util.Arrays.asList(activeProfiles).contains("dev");
    }
}