package org.example.howareyou.domain.member.repository;

import org.example.howareyou.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member,Long>{
    Optional<Member> findById(Long id);
    Optional<Member> findByMembername(String membername);
    boolean existsByMembername(String Membername);
}