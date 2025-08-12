package org.example.howareyou.domain.quiz.repository;

import org.example.howareyou.domain.quiz.entity.QuizResult;
import org.example.howareyou.domain.quiz.entity.QuizType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
/**
 * 조회/업데이트
 * */
public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {

    /* uuid 로 결과 찾기 */
    Optional<QuizResult> findByUuid(String uuid);

    /* 푼 퀴즈 중 최신것 가져오기 */
    @Query("""
        SELECT q
          FROM QuizResult q
         WHERE q.memberId = :memberId
           AND q.quizType = :quizType
           AND q.isRequiz = false
           AND (:dailyQuizUtcStart IS NULL OR q.dailyQuiz = :dailyQuizUtcStart)
         ORDER BY q.createdAt DESC
    """)
    Optional<QuizResult> findLatestOriginal(
            @Param("memberId") Long memberId,
            @Param("quizType") QuizType quizType,
            @Param("dailyQuizUtcStart") Instant dailyQuizUtcStart
    );

    /* 퀴즈 재도전 횟수 세기 */
    @Query("""
        SELECT COUNT(q)
          FROM QuizResult q
         WHERE q.memberId = :memberId
           AND (q.id = :originalId OR q.originalQuizId = :originalId)
    """)
    long countAttempts(@Param("memberId") Long memberId, @Param("originalId") Long originalId);

    /* 퀴즈 제출됐는지 확인 */
    @Query("""
    select qr.completed
      from QuizResult qr
     where qr.uuid = :uuid
""")
    Optional<Boolean> findCompletedQuizByUuid(@Param("uuid") String uuid);


    /* 채점 결과 집계 업데이트 (uuid) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update QuizResult qr
       set qr.correctCount  = :correct,
           qr.totalQuestions = :total,
           qr.score          = :score,
           qr.completed      = true,
           qr.completedAt    = :completedAt
     where qr.uuid = :uuid
""")
    int finalizeGradingByUuid(@Param("uuid") String uuid,
                              @Param("correct") long correct,
                              @Param("total") long total,
                              @Param("score") long score,
                              @Param("completedAt") Instant completedAt);
}