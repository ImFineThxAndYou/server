package org.example.howareyou.domain.member.service;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.dto.request.FilterRequest;
import org.example.howareyou.domain.member.dto.request.MembernameRequest;
import org.example.howareyou.domain.member.dto.request.ProfileCreateRequest;
import org.example.howareyou.domain.member.dto.response.MemberProfileViewForVoca;
import org.example.howareyou.domain.member.dto.response.MemberStatusResponse;
import org.example.howareyou.domain.member.dto.response.MembernameResponse;
import org.example.howareyou.domain.member.dto.response.ProfileResponse;
import org.example.howareyou.domain.member.entity.Category;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.entity.MemberProfile;
import org.example.howareyou.domain.member.redis.MemberCache;
import org.example.howareyou.domain.member.redis.MemberCacheService;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MemberCacheService memberCacheService;

    /* ---------- 프로필 ---------- */

    @Override
    public ProfileResponse getMyProfile(Long id) {
        return ProfileResponse.from(fetchMember(id));
    }

    @Override
    @Transactional
    public ProfileResponse updateMyProfile(Long id, @Valid ProfileCreateRequest r) {
        Member m  = fetchMember(id);
        MemberProfile p = m.getProfile();

        if (p == null) {                                 // 신규
            p = MemberProfile.create(
                    r.getNickname(), r.getAvatarUrl(),
                    r.getStatusMessage(), r.getInterests());
            p.setMember(m);
        } else {                                         // 수정
            p.updateProfile(
                    r.getNickname(), r.getStatusMessage(), r.getAvatarUrl(),
                    r.getInterests(), r.getBirthDate(),
                    r.getCountry(),  r.getRegion(),
                    r.getLanguage(), r.getTimezone());
        }

        if (!p.isCompleted()) p.completeProfile();
        memberCacheService.cache(m);       // 캐시 동기화

        return ProfileResponse.from(p);
    }

    @Override
    public ProfileResponse getPublicProfile(String membername){
        Member m = fetchMember(membername);
        return memberCacheService.get(m.getId())
                .map(this::fromCache)        // hit
                .orElseGet(() -> {           // miss → DB + 캐시
                    memberCacheService.cache(m);
                    return ProfileResponse.from(m.getProfile());
                });
    }

    @Override @Transactional
    public MembernameResponse setMembername(Long id, MembernameRequest req, HttpServletResponse res) {
        if (isMembernameDuplicated(req.membername()))
            throw new CustomException(ErrorCode.DUPLICATED_MEMBERNAME);

        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        m.setMembername(req.membername());
        return MembernameResponse.from(m);            // dirty-checking flush
    }

    @Override
    public boolean isMembernameDuplicated(String Membername){
        return memberRepository.existsByMembername(Membername.trim().toLowerCase());
    }

    /* ---------- Presence ---------- */

//    @Override
//    @Transactional
//    public void updatePresence(Long id, boolean online) {
//        cacheSvc.updateMemberState(id, online);
//    }

    @Override
        public MemberStatusResponse getMemberStatus(Long id){
            Member m = fetchMember(id);
            String membername = m.getMembername();

            return memberCacheService.get(id)
                    .map(mc -> MemberStatusResponse.builder()
                            .membername(membername)
                            .online(true)
                            .lastActiveAt(mc.getLastActiveAt())
                            .profileCompleted(mc.isCompleted())
                            .build())
                    .orElse( MemberStatusResponse.builder()
                            .membername(membername)
                            .online(false)
                            .lastActiveAt(m.getLastActiveAt()) // DB fallback
                            .profileCompleted(m.isProfileCompleted())
                            .build());
        }

    @Override
    public MemberStatusResponse getMemberStatus(String membername){
        Member m = fetchMember(membername);
        Long id  = m.getId();

        return memberCacheService.get(id)
                .map(mc -> MemberStatusResponse.builder()
                        .membername(membername)
                        .online(true)
                        .lastActiveAt(mc.getLastActiveAt())
                        .profileCompleted(mc.isCompleted())
                        .build())
                .orElse( MemberStatusResponse.builder()
                        .membername(membername)
                        .online(false)
                        .lastActiveAt(m.getLastActiveAt()) // DB fallback
                        .profileCompleted(m.isProfileCompleted())
                        .build());
    }

    @Override
    public Long getIdByMembername(String membername) {
        Member member = memberRepository.findByMembername(membername)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND, 
                        String.format("사용자를 찾을 수 없습니다: membername=%s", membername)));
        return member.getId();
    }

    /* ---------- 계정 ---------- */

    @Override
    @Transactional
    public void deleteAccount(Long id) {
        Member m = fetchMember(id);
        m.deleteAccount();
        memberCacheService.delete(id);
    }

    /* ---------- Related Users (같은 카테고리 사용자) ---------- */

    @Override
    @Transactional
    public List<ProfileResponse> findOthersWithSameCategories(Long requesterId) {
        Member me = fetchMember(requesterId);
        MemberProfile myProfile = me.getProfile();
        if (myProfile == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND); // 필요 시 정의
        }

        // 예: 관심사(카테고리)를 기준으로 다른 사용자 찾기
        Set<Category> interests = myProfile.getInterests(); //
        List<ProfileResponse> members =   memberRepository.findDistinctByProfileInterestsInAndIdNot(interests, requesterId)
                .stream()
                .map(ProfileResponse::from)
                .toList();
        return members;
    }
    /* ---------- Filter로 사용자 찾기 ---------- */
    @Override
    @Transactional
    public List<ProfileResponse> findOthersWithFilter(FilterRequest filter,Long requesterId){
        Set<Category> interestsEnums = new HashSet<>(filter.getInterests());
        Set<String> interests = interestsEnums.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        if (interests.isEmpty()) {
            // 관심사 필터가 비어있으면, requester만 제외하고 전부 리턴
            return memberRepository.findAll()
                    .stream()
                    .map(ProfileResponse::from)
                    .toList();

        }
        List<Member> members =  memberRepository.findByInterestsContainingAll(interests, requesterId, interests.size());
        return members.stream()
                .map(ProfileResponse::from)
                //.filter(MemberProfile::isCompleted) 나중에 주석해제 테스트 단계에서는 무시해도 됌
                .toList();
    }

    @Override
    public Member getMemberById(Long id) {
        return fetchMember(id);
    }

    @Override
    public Member getMemberByMembername(String membername) {
        return fetchMember(membername);
    }

    /* ---------- util ---------- */

    private Member fetchMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private Member fetchMember(String membername) {
        return memberRepository.findByMembername(membername)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private ProfileResponse fromCache(MemberCache c) {
        return ProfileResponse.builder()
                .membername(c.getMembername())
                .nickname(c.getNickname())
                .avatarUrl(c.getAvatarUrl())
                .bio(c.getBio())
                .interests(c.getInterests())
                .completed(c.isCompleted())
                .language(c.getLanguage())
                .timezone(c.getTimezone())
                .birthDate(c.getBirthDate())
                .age(c.getAge())
                .country(c.getCountry())
                .region(c.getRegion())
                .build();
    }


    /*단어장 생성용 조회*/

    @Override
    public List<MemberProfileViewForVoca> findAllActiveProfilesForVoca() {
        return memberRepository.findAllActiveProfilesForVoca();
    }
}