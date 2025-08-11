package org.example.howareyou.domain.member.dto.response;

public record MemberProfileViewForVoca(
        Long memberId,
        String membername,
        String language,
        String timezone
) {}
