package org.example.howareyou.domain.quiz.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.howareyou.domain.quiz.entity.QuizWord;

import java.util.List;

/**
 * 클라이언트에 내려줄 문제
 */
@Getter @Setter
@Builder
public class ClientQuizQuestion {
    private String question; //
    private List<String> choices; //
    private int questionNo; // 1-5

    /**
     * QuizWord 엔티티에서 ClientQuizQuestion으로 변환
     */
    public static ClientQuizQuestion from(QuizWord quizWord) {
        return ClientQuizQuestion.builder()
                .question(quizWord.getWord())
                .choices(List.of(
                    quizWord.getChoice1(),
                    quizWord.getChoice2(),
                    quizWord.getChoice3(),
                    quizWord.getChoice4()
                ))
                .questionNo(quizWord.getQuestionNo())
                .build();
    }
}
