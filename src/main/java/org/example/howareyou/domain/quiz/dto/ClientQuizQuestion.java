package org.example.howareyou.domain.quiz.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
}
