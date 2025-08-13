package org.example.howareyou.domain.recommendationtag.repository;

import java.util.List;
import java.util.Optional;
import org.example.howareyou.domain.member.entity.MemberTag;
import org.example.howareyou.domain.recommendationtag.entity.MemberTagScore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberTagScoreRepository extends JpaRepository<MemberTagScore, Long> {
  List<MemberTagScore> findByMemberId(Long memberId);
  Optional<MemberTagScore> findByMemberIdAndMemberTag(Long memberId, MemberTag memberTag);
  void deleteByMemberIdAndMemberTagNotIn(Long memberId, List<MemberTag> MemberTags);
}
