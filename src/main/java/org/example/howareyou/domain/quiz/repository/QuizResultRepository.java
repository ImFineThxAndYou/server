package org.example.howareyou.domain.quiz.repository;

import org.example.howareyou.domain.quiz.dto.response.QuizResponse;
import org.example.howareyou.domain.quiz.dto.response.WrongAnswerResponse;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    /* UUID 로 퀴즈 단건조회 */
    Optional<QuizResult> findByUuid(String uuid);

    /* UUID와 멤버 ID로 퀴즈 조회 */
    Optional<QuizResult> findByUuidAndMemberId(String uuid, Long memberId);

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

    /* 퀴즈 결과 업데이트 (새로운 메서드) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update QuizResult qr
       set qr.correctCount   = :correct,
           qr.totalQuestions = :total,
           qr.score          = :score,
           qr.completed      = :completed
     where qr.id = :quizResultId
""")
    int updateQuizResult(@Param("quizResultId") Long quizResultId,
                         @Param("correct") int correct,
                         @Param("total") int total,
                         @Param("score") int score,
                         @Param("completed") boolean completed);

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

    /* 회원ID와 상태로 퀴즈결과 조회 (새로운 메서드) */
    @Query("""
        select qr
          from QuizResult qr
         where qr.memberId = :memberId
           and qr.quizStatus = :status
    """)
    Page<QuizResult> findByMemberIdAndStatus(
            @Param("memberId") Long memberId,
            @Param("status") QuizStatus status,
            Pageable pageable
    );
    //가장 최근에 제출한 퀴즈 조회
    Optional<QuizResult> findTopByMemberIdAndQuizStatusOrderByCreatedAtDesc(
            Long memberId,
            QuizStatus quizStatus
    );

    /* -------------------- 대시보드용 메서드들 -------------------- */

    /**
     * 기간별 연속 학습일 계산
     */
    @Query(value = """
        SELECT COUNT(DISTINCT (qr.completed_at AT TIME ZONE :zoneId)::date)
          FROM quiz_result qr
         WHERE qr.member_id = :memberId
           AND qr.completed = true
           AND ((qr.completed_at AT TIME ZONE :zoneId)::date) BETWEEN :fromDate AND :toDate
    """, nativeQuery = true)
    int calculateLearningStreakByPeriod(@Param("memberId") Long memberId, 
                                        @Param("fromDate") LocalDate fromDate,
                                        @Param("toDate") LocalDate toDate,
                                        @Param("zoneId") String zoneId);

    /**
     * 기간별 퀴즈 응시 날짜 수 계산
     */
    @Query(value = """
        SELECT COUNT(DISTINCT (qr.completed_at AT TIME ZONE :zoneId)::date)
          FROM quiz_result qr
         WHERE qr.member_id = :memberId
           AND qr.completed = true
           AND ((qr.completed_at AT TIME ZONE :zoneId)::date) BETWEEN :fromDate AND :toDate
    """, nativeQuery = true)
    int countQuizDaysByPeriod(@Param("memberId") Long memberId, 
                              @Param("fromDate") LocalDate fromDate,
                              @Param("toDate") LocalDate toDate,
                              @Param("zoneId") String zoneId);

    /**
     * 기간별 점수 시리즈 조회
     */
    @Query("""
        SELECT qr
          FROM QuizResult qr
         WHERE qr.memberId = :memberId
           AND qr.completed = true
           AND qr.completedAt BETWEEN :fromUtc AND :toUtc
         ORDER BY qr.completedAt DESC
    """)
    List<QuizResult> findScoreSeriesByPeriod(@Param("memberId") Long memberId,
                                             @Param("fromUtc") Instant fromUtc,
                                             @Param("toUtc") Instant toUtc);

    /**
     * 기간별 일일 퀴즈 개수 조회
     */
    @Query(value = """
        SELECT 
            (qr.completed_at AT TIME ZONE :zoneId)::date as date,
            COUNT(*) as count
          FROM quiz_result qr
         WHERE qr.member_id = :memberId
           AND qr.completed = true
           AND ((qr.completed_at AT TIME ZONE :zoneId)::date) BETWEEN :fromDate AND :toDate
         GROUP BY (qr.completed_at AT TIME ZONE :zoneId)::date
         ORDER BY date
    """, nativeQuery = true)
    List<DailyQuizCount> getDailyQuizCountsByPeriod(@Param("memberId") Long memberId,
                                                    @Param("fromDate") LocalDate fromDate,
                                                    @Param("toDate") LocalDate toDate,
                                                    @Param("zoneId") String zoneId);

    /**
     * 최근 오답 조회
     */
    @Query("""
        SELECT new org.example.howareyou.domain.quiz.dto.response.WrongAnswerResponse(
            qw.word, qw.meaning, qw.pos
        )
          FROM QuizResult qr
          JOIN qr.quizWords qw
         WHERE qr.memberId = :memberId
           AND qr.completed = true
           AND qw.isCorrect = false
         ORDER BY qr.completedAt DESC
    """)
    List<WrongAnswerResponse> findLatestWrongAnswers(@Param("memberId") Long memberId);

    /**
     * 일일 퀴즈 개수 결과를 위한 인터페이스
     */
    interface DailyQuizCount {
        String getDate();
        Integer getCount();
    }
}