package org.example.howareyou.global.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.member.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** SecurityContext 에 저장·주입되는 사용자 객체 */
@RequiredArgsConstructor
@Getter
public class CustomMemberDetails implements UserDetails {

    private final Long id;                  // PK
    private final String email;             // 로그인 이메일
    private final String membername;       // membername 없으면 email
    private final boolean active;           // 휴면·탈퇴 여부
    private final Role role;

    private final List<GrantedAuthority> authorities;

    public CustomMemberDetails(Long id, String email, String membername, boolean active, Role role) {
        this.id = id;
        this.email = email;
        this.membername = membername;
        this.active = active;
        this.role = role;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /* ===== UserDetails 인터페이스 ===== */

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /** 비밀번호 기반 로그인 안 쓰므로 빈 문자열 반환 */
    @Override public String getPassword() { return ""; }

    /** Spring 내부 username → 사람이 알아보기 쉬운 값 */
    @Override public String getUsername() { return membername; }

    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isAccountNonLocked()     { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }

    /** 회원 활성 플래그와 연결 */
    @Override public boolean isEnabled() { return active; }
}