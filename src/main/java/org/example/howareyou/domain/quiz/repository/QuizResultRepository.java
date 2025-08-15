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
    /* UUID 로 퀴즈 단건조회 */
    Optional<QuizResult> findByUuid(String uuid);

    /* UUID 로 퀴즈 완료여부 조회*/
    @Query("""
        select qr.completed
          from QuizResult qr
         where qr.uuid = :uuid
    """)
    Optional<Boolean> findCompletedQuizByUuid(@Param("uuid") String uuid);

    /* 퀴즈 채점결과 및 상태변경 (quizStatus PENDING -> SUBMIT)*/
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

    /* 회원 ID (+상태) 로 퀴즈 결과 조회 (status == null 이면 모든상태 조회 - PENDING + SUBMIT)*/
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

    /* 회원ID 와 상태로 퀴즈결과페이지 조회 */
    Page<QuizResult> findByMemberIdAndQuizStatus(Long memberId, QuizStatus status, Pageable pageable);

    /* 회원ID 로 모든 퀴즈 결과 페이지 조회 */
    Page<QuizResult> findByMemberId(Long memberId, Pageable pageable);
}