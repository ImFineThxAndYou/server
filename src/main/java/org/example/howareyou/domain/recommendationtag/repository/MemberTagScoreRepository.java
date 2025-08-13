package org.example.howareyou.domain.recommendationtag.repository;

import org.example.howareyou.domain.recommendationtag.entity.MemberTagScore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberTagScoreRepository extends JpaRepository<MemberTagScore, Long> {
}
