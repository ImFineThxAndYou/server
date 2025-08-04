package org.example.howareyou.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.*;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** SecurityContext 에 저장·주입되는 사용자 객체 */
@RequiredArgsConstructor
public class CustomMemberDetails implements UserDetails {
    private final Long id;                    // = Member.id

    /** 사용자 ID 반환 (편의 메서드) */
    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){return List.of();}

    @Override
    public String getPassword(){return "";}

    @Override
    public String getUsername(){return id.toString();}

    @Override
    public boolean isAccountNonExpired(){return true;}

    @Override
    public boolean isAccountNonLocked(){return true;}

    @Override
    public boolean isCredentialsNonExpired(){return true;}

    @Override
    public boolean isEnabled(){return true;}
    public Long id(){return id;}
}