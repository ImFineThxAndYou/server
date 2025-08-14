package org.example.howareyou.domain.quiz.dto;

import lombok.*;

import java.util.List;


@Getter
@Setter
@Builder
public class QuizQuestion {
    private String question;
    private List<String> choices;
    private int answerIndex;

}
