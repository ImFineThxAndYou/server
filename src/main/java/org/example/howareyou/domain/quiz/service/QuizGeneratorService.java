package org.example.howareyou.domain.quiz.service;

import org.example.howareyou.domain.quiz.dto.ClientStartResponse;
import org.example.howareyou.domain.quiz.dto.response.QuizResultResponse;
import org.example.howareyou.domain.quiz.entity.QuizLevel;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


public interface QuizGeneratorService {
    // 1. 전체랜덤 퀴즈
    ClientStartResponse startRandomQuiz(String membername,  QuizLevel quizLevel);
    // 2. 날짜별 퀴즈
    ClientStartResponse startDailyQuiz(String membername, LocalDate date);


}
