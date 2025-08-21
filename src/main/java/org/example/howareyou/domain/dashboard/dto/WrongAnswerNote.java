package org.example.howareyou.domain.dashboard.dto;

import java.time.Instant;

public record WrongAnswerNote(
        String word,           // 단어
        String meaning,        // 의미
        String partOfSpeech,   // 품사
        String userAnswer,     // 사용자 답안
        String correctAnswer,  // 정답
        Instant quizDate       // 퀴즈 날짜
) {}
