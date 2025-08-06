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
    @Query(value = """
SELECT m.*
  FROM members m
  JOIN member_profiles mp ON mp.member_id = m.id
  JOIN member_interests mi ON mi.member_id = m.id
 WHERE m.id <> :requesterId
 GROUP BY m.id
HAVING
  COUNT(DISTINCT CASE WHEN mi.interest IN (:interests) THEN mi.interest END) = :interestCount
""", nativeQuery = true)
    List<Member> findByInterestsContainingAll(
            @Param("interests") Set<String> interests,
            @Param("requesterId") Long requesterId,
            @Param("interestCount") int interestCount
    );
}