package org.example.howareyou.domain.quiz.dto.response;
/* 개별 문항 정보 DTO*/

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResponse {
    private int questionNo;
    private String word;
    private String meaning;
    private String choice1;
    private String choice2;
    private String choice3;
    private String choice4;
    private Integer correctAnswer;   // 정답
    private Integer userAnswer;      // 유저가 기입한 정답
    private Boolean isCorrect;       // 정답여부
    private String level;            // 레벨
    private String pos;              // 품사
}
