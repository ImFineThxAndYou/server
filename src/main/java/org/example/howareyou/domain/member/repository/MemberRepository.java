package org.example.howareyou.domain.member.repository;

import org.example.howareyou.domain.member.dto.response.MemberProfileViewForVoca;
import org.example.howareyou.domain.member.entity.MemberTag;
import org.example.howareyou.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MemberRepository extends JpaRepository<Member,Long>{
    Optional<Member> findById(Long id);
    Optional<Member> findByMembername(String membername);
    boolean existsByMembername(String Membername);
    List<Member> findDistinctByProfileInterestsInAndIdNot(Set<MemberTag> interests, Long excludeId);

    Long getIdByMembername(String membername);

    @Modifying
    @Query("update Member m set m.lastActiveAt = :now where m.id = :id")
    void updateLastActive(@Param("id") Long id, @Param("now") Instant now);

    /**
     * Member와 Profile, Interests를 함께 조회 (Lazy Loading 방지)
     */
    @Query("SELECT DISTINCT m FROM Member m " +
           "LEFT JOIN FETCH m.profile p " +
           "LEFT JOIN FETCH p.interests " +
           "WHERE m.id = :id")
    Optional<Member> findByIdWithProfileAndInterests(@Param("id") Long id);

    /**
     * interests 에 주어진 모든 카테고리를 포함하고,
     * member.id ≠ :requesterId 인 프로필만 조회합니다.
     */
    @Query("SELECT DISTINCT m FROM Member m " +
           "LEFT JOIN FETCH m.profile p " +
           "LEFT JOIN FETCH p.interests " +
           "WHERE m.membername = :membername")
    Optional<Member> findByMembernameWithProfileAndInterests(@Param("membername") String membername);

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


    @Query("""
        select new org.example.howareyou.domain.member.dto.response.MemberProfileViewForVoca(
            m.id,
            m.membername,
            p.language,
            p.timezone
        )
        from Member m
        join m.profile p
        where m.active = true
    """)
    List<MemberProfileViewForVoca> findAllActiveProfilesForVoca();

    @Query("""
        select new org.example.howareyou.domain.member.dto.response.MemberProfileViewForVoca(
            m.id,
            m.membername,
            p.language,
            p.timezone
        )
        from Member m
        join m.profile p
        where m.active = true
    """)
    Page<MemberProfileViewForVoca> findAllActiveProfilesForVoca(Pageable pageable);
}