package org.example.howareyou.global.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.example.howareyou.domain.auth.repository.AuthRepository;
import org.example.howareyou.domain.member.entity.Member;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** JwtAuthFilter 가 호출하는 UserDetailsProvider */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CustomMemberDetailsService implements UserDetailsService {
    private final MemberRepository memberRepo;
    private final AuthRepository authRepo;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        log.debug("CustomMemberDetailsService.loadUserByUsername 호출: identifier={}", identifier);
        
        // identifier가 null이거나 빈 문자열인 경우 처리
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new UsernameNotFoundException("Identifier cannot be null or empty");
        }
        
        // identifier가 숫자인지 확인 (Auth ID인지 확인)
        if (identifier.matches("\\d+")) {
            Long authId = Long.valueOf(identifier);
            log.debug("Auth ID 기반 사용자 조회: {}", authId);
            
            // Auth ID로 Auth 엔티티 조회 후 Member 정보 가져오기 (eager fetch)
            return authRepo.findByIdWithMember(authId)
                    .map(auth -> {
                        if (auth.getMember() == null) {
                            throw new UsernameNotFoundException("Auth found but no member associated: " + authId);
                        }
                        Member member = auth.getMember();
                        return new CustomMemberDetails(
                                member.getId(),
                                member.getEmail(),
                                member.getMembername(),
                                member.isActive(),
                                member.getRole()
                        );
                    })
                    .orElseThrow(() -> new UsernameNotFoundException("Auth not found by ID: " + authId));
        } else if (identifier.contains("@")) {
            // email으로 사용자 찾기 (eager fetch)
            log.debug("Email 기반 사용자 조회: {}", identifier);
            return memberRepo.findByEmailForAuth(identifier)
                    .map(m -> new CustomMemberDetails(
                            m.getId(),
                            m.getEmail(),
                            m.getMembername(),
                            m.isActive(),
                            m.getRole()
                    ))
                    .orElseThrow(() -> new UsernameNotFoundException("Member not found by email: " + identifier));
        } else {
            // membername으로 사용자 찾기 (eager fetch)
            log.debug("Membername 기반 사용자 조회: {}", identifier);
            return memberRepo.findByMembernameForAuth(identifier)
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
