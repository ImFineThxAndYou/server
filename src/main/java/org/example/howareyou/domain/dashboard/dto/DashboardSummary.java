package org.example.howareyou.domain.dashboard.dto;

import org.example.howareyou.domain.quiz.dto.response.WrongAnswerResponse;

import java.util.List;

// record 는 불변객체, toString,getter,생성자 자동제공
public record DashboardSummary(
        long totalWords,
        int learningStreakDays,
        int reviewNeededDays,
        String encouragementMessage, // reviewNeededDays == 0 이면 "학습을 꾸준히 하시고계시는군요? 최고에요"
        List<ScorePoint> scoreSeries,
        List<WrongAnswerResponse> wrongAnswerNotes
) {}
