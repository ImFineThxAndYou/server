package org.example.howareyou.domain.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.howareyou.domain.auth.controller.dto.LoginDto;
import org.example.howareyou.domain.auth.controller.dto.TokenBundle;
import org.example.howareyou.domain.auth.entity.Auth;
import org.example.howareyou.domain.auth.entity.Provider;
import org.example.howareyou.domain.auth.repository.AuthRepository;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.entity.MemberProfile;
import org.example.howareyou.domain.member.entity.OnlineState;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123!";
    private static final String TEST_NICKNAME = "testuser";

    @BeforeEach
    void setUp() {
        // 테스트용 회원 및 인증 정보 생성
        MemberProfile profile = MemberProfile.builder()
                .nickname(TEST_NICKNAME)
                .completed(true)
                .build();

        Member member = Member.builder()
                .email(TEST_EMAIL)
                .profile(profile)
                .build();
        
        profile.setMember(member);
        Member savedMember = memberRepository.save(member);

        Auth auth = Auth.builder()
                .provider(Provider.LOCAL)
                .email(TEST_EMAIL)
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .member(savedMember)
                .build();
        
        authRepository.save(auth);
    }

    @Test
    void localLogin_Success() throws Exception {
        // given
        LoginDto loginDto = new LoginDto(TEST_EMAIL, TEST_PASSWORD);
        
        // when
        ResultActions result = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto))
        );

        // then
        result.andExpect(status().isOk())
              .andExpect(header().exists("Authorization"))
              .andExpect(cookie().exists("Refresh"));
        
        // 인증 정보가 올바르게 저장되었는지 확인
        Auth auth = authRepository.findByEmail(TEST_EMAIL).orElseThrow();
        assertThat(auth.getRefreshToken()).isNotNull();
        assertThat(auth.getRefreshTokenExpiry()).isAfter(LocalDateTime.now());
    }

    @Test
    void refreshToken_Success() throws Exception {
        // given - 로그인
        LoginDto loginDto = new LoginDto(TEST_EMAIL, TEST_PASSWORD);
        
        String refreshToken = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto))
        )
        .andReturn()
        .getResponse()
        .getCookie("Refresh")
        .getValue();

        // when - 토큰 갱신
        ResultActions result = mockMvc.perform(
            post("/api/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("Refresh", refreshToken))
        );

        // then
        result.andExpect(status().isOk())
              .andExpect(header().exists("Authorization"));
    }

    @Test
    void logout_Success() throws Exception {
        // given - 로그인
        LoginDto loginDto = new LoginDto(TEST_EMAIL, TEST_PASSWORD);
        
        String refreshToken = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto))
        )
        .andReturn()
        .getResponse()
        .getCookie("Refresh")
        .getValue();

        // when - 로그아웃
        ResultActions result = mockMvc.perform(
            post("/api/v1/auth/logout")
                .cookie(new jakarta.servlet.http.Cookie("Refresh", refreshToken))
        );

        // then
        result.andExpect(status().isOk());
        
        // 리프레시 토큰이 무효화되었는지 확인
        Auth auth = authRepository.findByEmail(TEST_EMAIL).orElseThrow();
        assertThat(auth.getRefreshToken()).isNull();
        assertThat(auth.getRefreshTokenExpiry()).isNull();
    }
}
