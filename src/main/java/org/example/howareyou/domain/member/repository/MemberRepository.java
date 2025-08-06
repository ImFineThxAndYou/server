package org.example.howareyou.domain.member.repository;

import org.example.howareyou.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member,Long>{
    Optional<Member> findById(Long id);
    Optional<Member> findByMembername(String membername);
    boolean existsByMembername(String Membername);

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
     * Membername으로 Member와 Profile, Interests를 함께 조회 (Lazy Loading 방지)
     */
    @Query("SELECT DISTINCT m FROM Member m " +
           "LEFT JOIN FETCH m.profile p " +
           "LEFT JOIN FETCH p.interests " +
           "WHERE m.membername = :membername")
    Optional<Member> findByMembernameWithProfileAndInterests(@Param("membername") String membername);
}