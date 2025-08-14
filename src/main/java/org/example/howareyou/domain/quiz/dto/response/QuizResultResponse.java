package org.example.howareyou.domain.quiz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.howareyou.domain.quiz.entity.QuizResult;
import org.example.howareyou.domain.quiz.entity.QuizType;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultResponse {
    private Long quizResultId;
    private QuizType quizType;       // RANDOM / DAILY
    private String dailyDate;        // DAILY: YYYY-MM-DD(UTC), RANDOM :  null
    private Instant createdAt;
    private Long totalQuestions;
    private Long correctCount;
    private Long score;
    private Double accuracy;         // 백분률 (correctCount / totalQuestions) * 100.0
    private List<QuizResponse> words;

    /** 엔티티 → DTO 변환 (퀴즈 단어까지 포함) */
    public static QuizResultResponse fromEntity(QuizResult qr) {
        String dailyDateStr = null;
        if (qr.getDailyQuiz() != null) {
            dailyDateStr = qr.getDailyQuiz().atOffset(ZoneOffset.UTC).toLocalDate().toString();
        }

        List<QuizResponse> wordDTOs = qr.getQuizWords().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(w -> w.getQuestionNo()))
                .map(w -> QuizResponse.builder()
                        .questionNo(w.getQuestionNo())
                        .word(nz(w.getWord()))
                        .meaning(nz(w.getMeaning()))
                        .choice1(nz(w.getChoice1()))
                        .choice2(nz(w.getChoice2()))
                        .choice3(nz(w.getChoice3()))
                        .choice4(nz(w.getChoice4()))
                        .correctAnswer(w.getCorrectAnswer())
                        .userAnswer(w.getUserAnswer())
                        .isCorrect(w.getIsCorrect())
                        .level(w.getLevel() == null ? "" : w.getLevel().name())
                        .pos(nz(w.getPos()))
                        .build())
                .toList();

        Double acc = null;
        if (qr.getTotalQuestions() != null && qr.getTotalQuestions() > 0 && qr.getCorrectCount() != null) {
            acc = (qr.getCorrectCount() * 100.0) / qr.getTotalQuestions();
        }

        return QuizResultResponse.builder()
                .quizResultId(qr.getId())
                .quizType(qr.getQuizType())
                .dailyDate(dailyDateStr)
                .createdAt(qr.getCreatedAt())
                .totalQuestions(qr.getTotalQuestions())
                .correctCount(qr.getCorrectCount())
                .score(qr.getScore())
                .accuracy(acc)
                .words(wordDTOs)
                .build();
    }

    private static String nz(String s) { return (s == null) ? "" : s; }
}