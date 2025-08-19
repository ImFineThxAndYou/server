package org.example.howareyou.global.security;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** JwtAuthFilter 가 호출하는 UserDetailsProvider */
@Service
@RequiredArgsConstructor
public class CustomMemberDetailsService implements UserDetailsService {
    private final MemberRepository memberRepo;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // identifier가 숫자인지 확인 (기존 ID 방식 지원)
        if (identifier.matches("\\d+")) {
            return memberRepo.findById(Long.valueOf(identifier))
                    .map(m -> new CustomMemberDetails(
                            m.getId(),
                            m.getEmail(),
                            m.getMembername(),
                            m.isActive(),
                            m.getRole()
                    ))
                    .orElseThrow(() -> new UsernameNotFoundException("Member not found by ID: " + identifier));
        } else {
            // membername으로 사용자 찾기
            return memberRepo.findByMembername(identifier)
                    .map(m -> new CustomMemberDetails(
                            m.getId(),
                            m.getEmail(),
                            m.getMembername(),
                            m.isActive(),
                            m.getRole()
                    ))
                    .orElseThrow(() -> new UsernameNotFoundException("Member not found by membername: " + identifier));
        }
    }
}
