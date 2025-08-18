package org.example.howareyou.domain.dashboard.dto;

import java.util.List;

/** 대시보드 통합 응답 DTO */
public record DashboardSummary(
        long totalWords,            // 총 단어 수
        int learningStreakDays,     // 오늘 기준 연속 학습일
        int reviewNeededDays,       // 복습 필요 일수
        String encouragementMessage,// 복습 필요 0일일 때 메시지
        List<ScorePoint> scoreSeries// 점수 시계열 (UTC)
) {}