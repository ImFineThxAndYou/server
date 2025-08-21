package org.example.howareyou.domain.quiz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WrongAnswerResponse {
    private String word;
    private String meaning;
    private String pos;
}