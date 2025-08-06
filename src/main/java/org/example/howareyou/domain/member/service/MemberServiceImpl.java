package org.example.howareyou.domain.member.service;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.member.dto.request.MembernameRequest;
import org.example.howareyou.domain.member.dto.request.ProfileCreateRequest;
import org.example.howareyou.domain.member.dto.response.MemberStatusResponse;
import org.example.howareyou.domain.member.dto.response.MembernameResponse;
import org.example.howareyou.domain.member.dto.response.ProfileResponse;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.entity.MemberProfile;
import org.example.howareyou.domain.member.redis.MemberCache;
import org.example.howareyou.domain.member.redis.MemberCacheService;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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

    /* ---------- 계정 ---------- */

    @Override
    @Transactional
    public void deleteAccount(Long id) {
        Member m = fetchMember(id);
        m.deleteAccount();
        memberCacheService.delete(id);
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
}