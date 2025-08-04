package org.example.howareyou.global.security;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

/** JwtAuthFilter 가 호출하는 UserDetailsProvider */
@Service
@RequiredArgsConstructor
public class CustomMemberDetailsService implements UserDetailsService {
    private final MemberRepository userRepo;
    @Override
    public UserDetails loadUserByUsername(String uid){
        return userRepo.findById(Long.valueOf(uid))
                .map(u->new CustomMemberDetails(u.getId()))
                .orElseThrow(()->new UsernameNotFoundException("user not found"));
    }
}
