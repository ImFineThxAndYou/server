package org.example.howareyou.domain.quiz.dto.submit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitResponse {
    private String quizUUID;   // 해당 퀴즈 UUID
    private int correctCount;    // 맞힌 개수
    private int totalQuestions;  // 총 문항 수
    private long score;          // 점수
}
