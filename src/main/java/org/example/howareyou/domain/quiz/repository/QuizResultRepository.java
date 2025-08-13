package org.example.howareyou.domain.quiz.repository;

import org.example.howareyou.domain.quiz.entity.QuizResult;
import org.example.howareyou.domain.quiz.entity.QuizType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {

    Optional<QuizResult> findByUuid(String uuid);

    // 랜덤: daily_quiz IS NULL
    @Query("""
        SELECT q
          FROM QuizResult q
         WHERE q.memberId = :memberId
           AND q.quizType = :quizType
           AND q.isRequiz = false
           AND q.dailyQuiz IS NULL
         ORDER BY q.createdAt DESC
    """)
    Optional<QuizResult> findLatestOriginalRandom(
            @Param("memberId") Long memberId,
            @Param("quizType") QuizType quizType
    );

    // 데일리: daily_quiz = :dailyQuizUtcStart (키가 있을 때만 사용)
    @Query("""
        SELECT q
          FROM QuizResult q
         WHERE q.memberId = :memberId
           AND q.quizType = :quizType
           AND q.isRequiz = false
           AND q.dailyQuiz = :dailyQuizUtcStart
         ORDER BY q.createdAt DESC
    """)
    Optional<QuizResult> findLatestOriginalDaily(
            @Param("memberId") Long memberId,
            @Param("quizType") QuizType quizType,
            @Param("dailyQuizUtcStart") Instant dailyQuizUtcStart
    );

    // 데일리인데 키가 null인 케이스를 분리 (원한다면 유지)
    @Query("""
        SELECT q
          FROM QuizResult q
         WHERE q.memberId = :memberId
           AND q.quizType = :quizType
           AND q.isRequiz = false
           AND q.dailyQuiz IS NULL
         ORDER BY q.createdAt DESC
    """)
    Optional<QuizResult> findLatestOriginalDailyIsNull(
            @Param("memberId") Long memberId,
            @Param("quizType") QuizType quizType
    );

    @Query("""
        SELECT COUNT(q)
          FROM QuizResult q
         WHERE (q.id = :originalId OR q.originalQuizId = :originalId)
    """)
    long countAttempts(@Param("originalId") Long originalId);

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
               qr.completedAt    = :completedAt
         where qr.uuid = :uuid
    """)
    int finalizeGradingByUuid(@Param("uuid") String uuid,
                              @Param("correct") long correct,
                              @Param("total") long total,
                              @Param("score") long score,
                              @Param("completedAt") Instant completedAt);
}