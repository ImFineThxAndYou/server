package org.example.howareyou.domain.quiz.service;

import org.example.howareyou.domain.quiz.dto.ClientStartResponse;

import java.time.LocalDate;


public interface QuizGeneratorService {
    // 1. 전체랜덤 퀴즈
    ClientStartResponse startRandomQuiz(Long memberId, String meaningLang, int count);
    // 2. 날짜별 퀴즈
    ClientStartResponse startDailyQuiz(Long memberId, LocalDate date, String meaningLang, int count);
}
