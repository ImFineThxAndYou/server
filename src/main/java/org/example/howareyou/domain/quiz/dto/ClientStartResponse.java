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
    private int maxRetry = 5;
    private int usedRetry; // 현재 몇버 풀었는지
    private int remainingRetry; // 기회 몇번 남앗는지 maxRetry - usedRetry

}
