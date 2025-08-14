package org.example.howareyou.domain.quiz.dto;

import lombok.*;

import java.util.List;

/**
 * 퀴즈 응답 */
@Getter
@Setter
@Builder
public class ClientStartResponse {
    private Long quizResultId; //생성된 퀴즈 ID
    private List<ClientQuizQuestion> quizQuestions; // 문제 리스트
    private String quizUUID;

}
