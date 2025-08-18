package org.example.howareyou.domain.quiz.repository;

import org.example.howareyou.domain.quiz.entity.QuizResult;
import org.example.howareyou.domain.quiz.entity.QuizStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    Page<QuizResult> findByMemberIdAndOptionalStatus(@Param("memberId") Long memberId,
                                                     @Param("status") QuizStatus status,
                                                     Pageable pageable);

    /* 회원ID 와 상태로 퀴즈결과페이지 조회 */
    Page<QuizResult> findByMemberIdAndQuizStatus(Long memberId, QuizStatus status, Pageable pageable);

    /* 회원ID 로 모든 퀴즈 결과 페이지 조회 */
    Page<QuizResult> findByMemberId(Long memberId, Pageable pageable);

    /* 생성날짜(LocalDate) 집합: 타임존 기준, distinct */
    @Query(value = """
        SELECT DISTINCT (qr.created_at AT TIME ZONE :tz)::date AS day
        FROM quiz_result qr
        WHERE qr.member_id = :memberId
          AND qr.created_at >= :fromUtc
          AND qr.created_at <  :toUtcExcl
    """, nativeQuery = true)
    List<java.sql.Date> findDistinctCreatedLocalDates(@Param("memberId") Long memberId,
                                                 @Param("fromUtc") Instant fromUtc,
                                                 @Param("toUtcExcl") Instant toUtcExcl,
                                                 @Param("tz") String tz);

    /* 제출날짜(LocalDate) 집합: SUBMIT만, 타임존 기준, distinct */
    @Query(value = """
        SELECT DISTINCT (qr.completed_at AT TIME ZONE :tz)::date AS day
        FROM quiz_result qr
        WHERE qr.member_id = :memberId
          AND qr.quiz_status = 'SUBMIT'
          AND qr.completed_at >= :fromUtc
          AND qr.completed_at <  :toUtcExcl
    """, nativeQuery = true)
    List<java.sql.Date> findDistinctSubmittedLocalDates(@Param("memberId") Long memberId,
                                                   @Param("fromUtc") Instant fromUtc,
                                                   @Param("toUtcExcl") Instant toUtcExcl,
                                                   @Param("tz") String tz);

    /* 점수 시계열(제출 이력): 기본 꺾은선 그래프용 */
    @Query(value = """
    SELECT qr.uuid AS uuid,
           qr.completed_at AS submittedAt,
           qr.score AS score
    FROM quiz_result qr
    WHERE qr.member_id = :memberId
      AND qr.quiz_status = 'SUBMIT'
      AND qr.completed_at >= :fromUtc
      AND qr.completed_at <  :toUtc
    ORDER BY qr.completed_at ASC
    LIMIT :limit
""", nativeQuery = true)
    List<AttemptRow> findAttempts(Long memberId, Instant fromUtc, Instant toUtc, int limit);

    interface AttemptRow {
        String getUuid();
        Instant getSubmittedAt();  // alias: submittedAt (completed_at)
        Long getScore();           // ← bigint(=Long) 추천
    }
}