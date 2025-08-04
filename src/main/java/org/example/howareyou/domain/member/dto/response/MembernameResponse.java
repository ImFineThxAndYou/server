package org.example.howareyou.domain.member.dto.response;

import org.example.howareyou.domain.member.entity.Member;

public record MembernameResponse(String username) {
    public static MembernameResponse from(Member m){
        return new MembernameResponse(m.getMembername());
    }
}