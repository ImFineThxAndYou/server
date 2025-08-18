package org.example.howareyou.domain.recommendationtag.repository;

import java.util.List;
import java.util.Optional;
import org.example.howareyou.domain.member.entity.MemberTag;
import org.example.howareyou.domain.recommendationtag.entity.MemberTagScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberTagScoreRepository extends JpaRepository<MemberTagScore, Long> {
  List<MemberTagScore> findByMemberId(Long memberId);
  Optional<MemberTagScore> findByMemberIdAndMemberTag(Long memberId, MemberTag memberTag);
  void deleteByMemberIdAndMemberTagNotIn(Long memberId, List<MemberTag> MemberTags);

  @Query("SELECT DISTINCT m.memberId FROM MemberTagScore m")
  List<Long> findDistinctMemberIds();

  @Query("SELECT DISTINCT m.memberId FROM MemberTagScore m WHERE m.memberId <> :memberId")
  List<Long> findDistinctMemberIdsExcept(@Param("memberId") Long memberId);

}
