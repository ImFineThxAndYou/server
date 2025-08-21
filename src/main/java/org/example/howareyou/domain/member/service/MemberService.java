package org.example.howareyou.domain.member.service;

import jakarta.servlet.http.HttpServletResponse;
import org.example.howareyou.domain.member.dto.request.FilterRequest;
import org.example.howareyou.domain.member.dto.request.MembernameRequest;
import org.example.howareyou.domain.member.dto.request.ProfileCreateRequest;
import org.example.howareyou.domain.member.dto.response.MemberProfileViewForVoca;
import org.example.howareyou.domain.member.dto.response.MembernameResponse;
import org.example.howareyou.domain.member.dto.response.ProfileResponse;
import org.example.howareyou.domain.member.dto.response.MemberStatusResponse;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.entity.MemberProfile;
import org.springframework.data.domain.Pageable;

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

    Long getIdByMembername(String membername);

    /* 계정 */
    void deleteAccount(Long id);

    List<ProfileResponse> findOthersWithSameCategories(Long requesterId);
    List<ProfileResponse> findOthersWithFilter(FilterRequest filterRequest,Long requesterId);

    /* get member */
    Member getMemberById(Long id);
    Member getMemberByMembername(String membername);

    /* 단어장 생성용 프로필 조회*/
    List<MemberProfileViewForVoca> findAllActiveProfilesForVoca();

    /* 대시보드용 메서드 */
    String findMembernameById(Long memberId);
}