package org.example.howareyou.domain.quiz.repository;

import org.example.howareyou.domain.quiz.dto.response.QuizResponse;
import org.example.howareyou.domain.quiz.entity.QuizResult;
import org.example.howareyou.domain.quiz.entity.QuizStatus;
import org.example.howareyou.domain.quiz.entity.QuizType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {

    Optional<QuizResult> findByUuid(String uuid);

    @Query("""
        select qr.completed
          from QuizResult qr
         where qr.uuid = :uuid
    """)
    Optional<Boolean> findCompletedQuizByUuid(@Param("uuid") String uuid);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update QuizResult qr
       set qr.correctCount   = :correct,
           qr.totalQuestions = :total,
           qr.score          = :score,
           qr.completed      = true,
           qr.completedAt    = :completedAt,
           qr.quizStatus     = :quizStatus
     where qr.uuid = :uuid
""")
    int finalizeGradingByUuid(@Param("uuid") String uuid,
                              @Param("correct") long correct,
                              @Param("total") long total,
                              @Param("score") long score,
                              @Param("completedAt") Instant completedAt,
                              @Param("quizStatus") QuizStatus quizStatus);

    @Query("""
        select qr
          from QuizResult qr
         where qr.memberId = :memberId
           and (:status is null or qr.quizStatus = :status)
    """)
    Page<QuizResult> findByMemberIdAndOptionalStatus(
            @Param("memberId") Long memberId,
            @Param("status") QuizStatus status,
            Pageable pageable
    );
}