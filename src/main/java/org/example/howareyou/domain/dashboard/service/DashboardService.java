package org.example.howareyou.domain.dashboard.service;

import org.example.howareyou.domain.dashboard.dto.ScorePoint;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public interface DashboardService {
    // 단어장 - 멤버별 총 단어 개수
    long countWords(String membername);
    // 연속 학습일 - 프론트엔드에서 타임존 받아야해요
    int getLearningDays(Long memberId, ZoneId zone);
    // 복습 필요한 날짜
    int countReviewDays(String membername, LocalDate from, LocalDate to, ZoneId zone);
    // 학습 성과 분석 - 확장성을 위해 from , to 는 변수로 받지만 고정 30일
    List<ScorePoint> getQuizScoreSeries(Long memberId, Instant fromUtc, Instant toUtc, Integer limit);



}
