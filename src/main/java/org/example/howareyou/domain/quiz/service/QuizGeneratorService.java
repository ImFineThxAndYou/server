package org.example.howareyou.domain.quiz.service;

import org.example.howareyou.domain.quiz.dto.ClientStartResponse;

import java.time.LocalDate;


public interface QuizGeneratorService {
    // 1. 전체랜덤 퀴즈
    ClientStartResponse startRandomQuiz(String membername, String language);
    // 2. 날짜별 퀴즈
    ClientStartResponse startDailyQuiz(String membername, LocalDate date, String language);
}
