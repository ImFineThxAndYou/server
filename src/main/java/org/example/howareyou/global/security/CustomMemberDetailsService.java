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
    public UserDetails loadUserByUsername(String uid) throws UsernameNotFoundException {
        return memberRepo.findById(Long.valueOf(uid))
                .map(m -> new CustomMemberDetails(
                        m.getId(),
                        m.getEmail(),
                        m.getMembername(),
                        m.isActive(),
                        m.getRole()
                ))
                .orElseThrow(() -> new UsernameNotFoundException("Member not found : " + uid));
    }
}
