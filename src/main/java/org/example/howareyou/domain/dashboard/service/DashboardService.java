package org.example.howareyou.domain.dashboard.service;

import org.example.howareyou.domain.dashboard.dto.ScorePoint;
import org.example.howareyou.domain.dashboard.dto.WrongAnswer;
import org.example.howareyou.domain.dashboard.dto.DashboardSummary;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public interface DashboardService {
    /**
     * 대시보드 요약 정보 조회
     * @param memberId 사용자 ID
     * @param zoneId 타임존
     * @param period 조회 기간 (week/month)
     * @return 대시보드 요약 정보
     */
    DashboardSummary getDashboardSummary(Long memberId, ZoneId zoneId, String period);

    /**
     * 단어 개수 조회
     * @param memberId 사용자 ID
     * @param lang 언어 필터 (선택사항)
     * @param pos 품사 필터 (선택사항)
     * @param period 조회 기간 (week/month)
     * @return 단어 개수
     */
    long countWords(Long memberId, String lang, String pos, String period);

    /**
     * 연속 학습일 조회
     * @param memberId 사용자 ID
     * @param zoneId 타임존
     * @param period 조회 기간 (week/month)
     * @return 연속 학습일
     */
    int getLearningDays(Long memberId, ZoneId zoneId, String period);

    /**
     * 복습 필요 날짜 조회
     * @param memberId 사용자 ID
     * @param from 시작 날짜
     * @param to 종료 날짜
     * @param zoneId 타임존
     * @return 복습 필요 날짜 수
     */
    int countReviewDays(Long memberId, LocalDate from, LocalDate to, ZoneId zoneId);

    /**
     * 학습 성과 분석 (점수 시리즈)
     * @param memberId 사용자 ID
     * @param fromUtc 시작 시간 (UTC)
     * @param toUtc 종료 시간 (UTC)
     * @param limit 조회 개수 제한
     * @return 점수 시리즈
     */
    List<ScorePoint> getQuizScoreSeries(Long memberId, Instant fromUtc, Instant toUtc, Integer limit);

    /**
     * 오답노트 조회
     * @param memberId 사용자 ID
     * @return 오답노트 목록
     */
    List<WrongAnswer> getWrongAnswer(Long memberId);

    /**
     * 학습 잔디 데이터 조회
     * @param memberId 사용자 ID
     * @param year 조회 연도
     * @param zoneId 타임존
     * @param period 조회 기간 (week/month)
     * @return 학습 잔디 데이터
     */
    Map<String, Integer> getLearningGrass(Long memberId, int year, ZoneId zoneId, String period);

    /**
     * 단어장 잔디 데이터 조회
     * @param memberId 사용자 ID
     * @param year 조회 연도
     * @param zoneId 타임존
     * @param period 조회 기간 (week/month)
     * @return 단어장 잔디 데이터
     */
    Map<String, Integer> getVocabularyGrass(Long memberId, int year, ZoneId zoneId, String period);

    /**
     * 퀴즈 잔디 데이터 조회
     * @param memberId 사용자 ID
     * @param year 조회 연도
     * @param zoneId 타임존
     * @param period 조회 기간 (week/month)
     * @return 퀴즈 잔디 데이터
     */
    Map<String, Integer> getQuizGrass(Long memberId, int year, ZoneId zoneId, String period);
}
