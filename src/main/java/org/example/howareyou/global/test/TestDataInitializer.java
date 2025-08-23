package org.example.howareyou.global.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.auth.entity.Auth;
import org.example.howareyou.domain.auth.entity.Provider;
import org.example.howareyou.domain.auth.repository.AuthRepository;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.entity.MemberProfile;
import org.example.howareyou.domain.member.entity.Role;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Set;

/**
 * 개발 환경에서 테스트용 데이터를 자동으로 초기화하는 컴포넌트
 * - dev 프로필에서만 동작
 * - 애플리케이션 시작 시 테스트 계정들을 자동 생성
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TestDataInitializer {

    private final MemberRepository memberRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initializeTestData() {
        return args -> {
            log.info("🧪 테스트 데이터 초기화 시작...");
            
            // 기본 테스트 계정들
            createTestUser("test@example.com", "password123!", "테스트유저", true);
            createTestUser("admin@example.com", "admin123!", "관리자", true);
            createTestUser("user1@example.com", "user123!", "사용자1", true);
            createTestUser("user2@example.com", "user123!", "사용자2", false); // 프로필 미완성
            
            log.info("✅ 테스트 데이터 초기화 완료!");
        };
    }

    /**
     * 테스트용 사용자 생성
     */
    private void createTestUser(String email, String password, String nickname, boolean profileCompleted) {
        // 이미 존재하는지 확인
        if (authRepository.findByEmailAndProvider(email, Provider.LOCAL).isPresent()) {
            log.debug("이미 존재하는 테스트 계정: {}", email);
            return;
        }

        // 프로필 생성
        MemberProfile profile = MemberProfile.builder()
                .nickname(nickname)
                .completed(profileCompleted)
                .avatarUrl("https://via.placeholder.com/150")
                .language("ko")
                .timezone("Asia/Seoul")
                .interests(Set.of(
                    // 테스트용 관심사 설정
                    org.example.howareyou.domain.member.entity.MemberTag.TECHNOLOGY,
                    org.example.howareyou.domain.member.entity.MemberTag.MUSIC,
                    org.example.howareyou.domain.member.entity.MemberTag.FOOD,
                    org.example.howareyou.domain.member.entity.MemberTag.TRAVEL
                ))
                .build();

        // 회원 생성
        Member member = Member.builder()
                .email(email)
                .membername(email.split("@")[0]) // 이메일 앞부분을 membername으로 사용
                .role(email.contains("admin") ? Role.ADMIN : Role.USER)
                .active(true)
                .lastActiveAt(Instant.now())
                .profile(profile)
                .build();
        
        profile.setMember(member);
        Member savedMember = memberRepository.save(member);

        // 인증 정보 생성
        Auth auth = Auth.builder()
                .provider(Provider.LOCAL)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .member(savedMember)
                .lastLoginAt(Instant.now())
                .build();
        
        authRepository.save(auth);
        
        log.info("테스트 계정 생성: {} (비밀번호: {})", email, password);
    }
} 