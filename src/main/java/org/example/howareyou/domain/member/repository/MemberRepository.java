package org.example.howareyou.domain.member.repository;

import org.example.howareyou.domain.member.entity.Category;
import org.example.howareyou.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MemberRepository extends JpaRepository<Member,Long>{
    Optional<Member> findById(Long id);
    Optional<Member> findByMembername(String membername);
    boolean existsByMembername(String Membername);

    List<Member> findDistinctByProfileInterestsInAndIdNot(Set<Category> interests, Long excludeId);


    /**
     * interests 에 주어진 모든 카테고리를 포함하고,
     * member.id ≠ :requesterId 인 프로필만 조회합니다.
     */
    @Query("""
  SELECT m
    FROM Member m
    JOIN m.profile mp
    JOIN mp.interests i
   WHERE i IN :interests
     AND m.id <> :requesterId
   GROUP BY m
  HAVING COUNT(DISTINCT i) = :#{#interests.size()}
""")
    List<Member> findByInterestsContainingAll(
            @Param("interests") Set<Category> interests,
            @Param("requesterId") Long requesterId
    );
}