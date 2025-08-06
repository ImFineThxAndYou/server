package org.example.howareyou.domain.member.service;

import jakarta.servlet.http.HttpServletResponse;
import org.example.howareyou.domain.member.dto.request.FilterRequest;
import org.example.howareyou.domain.member.dto.request.MembernameRequest;
import org.example.howareyou.domain.member.dto.request.ProfileCreateRequest;
import org.example.howareyou.domain.member.dto.response.MembernameResponse;
import org.example.howareyou.domain.member.dto.response.ProfileResponse;
import org.example.howareyou.domain.member.dto.response.MemberStatusResponse;
import org.example.howareyou.domain.member.entity.MemberProfile;

import java.util.List;
import java.util.UUID;

public interface MemberService {

    /* 프로필 */
    ProfileResponse getMyProfile(Long id);
    ProfileResponse updateMyProfile(Long id, ProfileCreateRequest request);
    ProfileResponse getPublicProfile(String membername);

    MembernameResponse setMembername(Long memberId, MembernameRequest req, HttpServletResponse res);
    boolean isMembernameDuplicated(String username);

//    /* Presence */
//    void updatePresence(Long id, boolean online);
    MemberStatusResponse getMemberStatus(Long id);
    MemberStatusResponse getMemberStatus(String membername);

    /* 계정 */
    void deleteAccount(Long id);

    List<MemberProfile> findOthersWithSameCategories(Long requesterId);
    List<MemberProfile> findOthersWithFilter(FilterRequest filterRequest,Long requesterId);
}