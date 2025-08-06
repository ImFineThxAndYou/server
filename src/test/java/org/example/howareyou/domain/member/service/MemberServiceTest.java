
package org.example.howareyou.domain.member.service;

import org.example.howareyou.domain.member.entity.Category;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.entity.MemberProfile;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberServiceImpl memberService;

    @Mock
    private MemberRepository memberRepository;

    private Member member1;
    private Member member2;
    private Member member3;

    @BeforeEach
    void setUp() {
        member1 = Member.builder().id(1L).build();
        MemberProfile profile1 = MemberProfile.builder()
                .interests(Set.of(Category.SPORTS, Category.MUSIC))
                .completed(true)
                .build();
        member1.setProfile(profile1);

        member2 = Member.builder().id(2L).build();
        MemberProfile profile2 = MemberProfile.builder()
                .interests(Set.of(Category.SPORTS, Category.MOVIE))
                .completed(true)
                .build();
        member2.setProfile(profile2);

        member3 = Member.builder().id(3L).build();
        MemberProfile profile3 = MemberProfile.builder()
                .interests(Set.of(Category.GAME, Category.IT))
                .completed(true)
                .build();
        member3.setProfile(profile3);
    }

    @Test
    void findOthersWithSameCategories() {
        // given
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member1));
        when(memberRepository.findDistinctByProfileInterestsInAndIdNot(
                Set.of(Category.SPORTS, Category.MUSIC), 1L))
                .thenReturn(List.of(member2));

        // when
        List<MemberProfile> result = memberService.findOthersWithSameCategories(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMember().getId()).isEqualTo(2L);
    }
}
