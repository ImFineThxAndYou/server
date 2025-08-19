package org.example.howareyou.domain.quiz.service;

import org.example.howareyou.domain.quiz.dto.ClientStartResponse;
import org.example.howareyou.domain.quiz.dto.response.QuizResultResponse;
import org.example.howareyou.domain.quiz.dto.response.WrongAnswerResponse;
import org.example.howareyou.domain.quiz.dto.submit.SubmitResponse;
import org.example.howareyou.domain.quiz.entity.QuizStatus;
import org.example.howareyou.domain.dashboard.dto.ScorePoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public interface QuizService {
    // 채점
    SubmitResponse gradeQuiz(String QuizeUUID, List<Integer> selectedIndices);

    // 회원별 퀴즈 전체조회
    @Transactional(readOnly = true)
    Page<QuizResultResponse> getQuizResultsByMember(Long memberId, QuizStatus status, Pageable pageable);

    // 회원별 퀴즈결과 상세조회
    QuizResultResponse getQuizResultDetail(String quizUUID);

    // 퀴즈 status 가 pending 인것만
    @Transactional(readOnly = true)
    ClientStartResponse getPendingQuizByUuid(Long memberId, String uuid);

    //가장 최근에 제출한 퀴즈 틀린 단어 가져오기
    @Transactional(readOnly = true)
    List<WrongAnswerResponse> getWrongAnswer(Long memberId);

    /* -------------------- 대시보드용 메서드들 -------------------- */

    /**
     * 연속 학습일 계산 (기간별)
     * @param memberId 사용자 ID
     * @param zoneId 타임존
     * @param period 조회 기간 (week/month)
     * @return 연속 학습일
     */
    @Transactional(readOnly = true)
    int calculateLearningStreak(Long memberId, ZoneId zoneId, String period);

    /**
     * 복습 필요 날짜 계산
     * @param memberId 사용자 ID
     * @param from 시작 날짜
     * @param to 종료 날짜
     * @param zoneId 타임존
     * @return 복습 필요 날짜 수
     */
    @Transactional(readOnly = true)
    int countReviewNeededDays(Long memberId, LocalDate from, LocalDate to, ZoneId zoneId);

    /**
     * 점수 시리즈 조회
     * @param memberId 사용자 ID
     * @param fromUtc 시작 시간 (UTC)
     * @param toUtc 종료 시간 (UTC)
     * @param limit 조회 개수 제한
     * @return 점수 시리즈
     */
    @Transactional(readOnly = true)
    List<ScorePoint> getScoreSeries(Long memberId, Instant fromUtc, Instant toUtc, Integer limit);

    /**
     * 퀴즈 잔디 데이터 조회 (GitHub 스타일)
     * @param memberId 사용자 ID
     * @param year 조회 연도
     * @param zoneId 타임존
     * @param period 조회 기간 (week/month)
     * @return 퀴즈 잔디 데이터
     */
    @Transactional(readOnly = true)
    Map<String, Integer> getQuizGrass(Long memberId, int year, ZoneId zoneId, String period);
}
