package org.example.howareyou.domain.member.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.member.dto.request.MembernameRequest;
import org.example.howareyou.domain.member.dto.request.ProfileCreateRequest;
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

import java.util.List;
import java.util.Set;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository   memberRepo;
    private final MemberCacheService cacheSvc;

    /* ---------- 프로필 ---------- */

    @Override
    public ProfileResponse getMyProfile(Long id) {
        return ProfileResponse.from(fetchMember(id).getProfile());
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
        cacheSvc.cacheMember(MemberCache.from(m));       // 캐시 동기화

        return ProfileResponse.from(p);
    }

    @Override
    public ProfileResponse getPublicProfile(String membername) {
        return ProfileResponse.from(fetchMember(membername).getProfile());
    }

    @Override @Transactional
    public MembernameResponse setMembername(Long id, MembernameRequest req){
        if (isMembernameDuplicated(req.membername()))
            throw new CustomException(ErrorCode.DUPLICATED_MEMBERNAME);

        Member m = memberRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        m.setMembername(req.membername());
        return MembernameResponse.from(m);            // dirty-checking flush
    }

    @Override
    public boolean isMembernameDuplicated(String Membername){
        return memberRepo.existsByMembername(Membername.trim().toLowerCase());
    }

    /* ---------- Presence ---------- */

    @Override
    @Transactional
    public void updatePresence(Long id, boolean online) {
        cacheSvc.updateMemberState(id, online);
    }

    @Override
    public MemberStatusResponse getMemberStatus(Long id) {
        Member m = fetchMember(id);
        return MemberStatusResponse.builder()
                .memberId(id)
                .online(cacheSvc.isMemberOnline(id))
                .lastActiveAt(cacheSvc.getLastActiveAt(id))
                .profileCompleted(m.isProfileCompleted())
                .build();
    }

    @Override
    public MemberStatusResponse getMemberStatus(String membername) {
        Member m = fetchMember(membername);
        Long id = m.getId();
        return MemberStatusResponse.builder()
                .memberId(id)
                .online(cacheSvc.isMemberOnline(id))
                .lastActiveAt(cacheSvc.getLastActiveAt(id))
                .profileCompleted(m.isProfileCompleted())
                .build();
    }

    /* ---------- 계정 ---------- */

    @Override
    @Transactional
    public void deleteAccount(Long id) {
        Member m = fetchMember(id);
        m.deleteAccount();
        cacheSvc.delete(id);
    }

    /* ---------- Related Users (같은 카테고리 사용자) ---------- */

    @Override
    @Transactional(readOnly = true)
    public List<MemberProfile> findOthersWithSameCategories(Long requesterId) {
        Member me = fetchMember(requesterId);
        MemberProfile myProfile = me.getProfile();
        if (myProfile == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND); // 필요 시 정의
        }

        // 예: 관심사(카테고리)를 기준으로 다른 사용자 찾기
        Set<Category> interests = myProfile.getInterests(); //
        List<Member> members =   memberRepo.findDistinctByProfileInterestsInAndIdNot(interests, requesterId);
        return members.stream()
                .map(Member::getProfile)
                .filter(MemberProfile::isCompleted)
                .toList();


    }

    /* ---------- util ---------- */

    private Member fetchMember(Long id) {
        return memberRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private Member fetchMember(String membername) {
        return memberRepo.findByMembername(membername)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

}