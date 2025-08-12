package org.example.howareyou.domain.quiz.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.howareyou.domain.quiz.dto.membervoca.MemberVocabWordQuiz;

@Getter @Setter @Builder
public class RandomQuizRequest {
    @NotNull
    private Long memberId;

    /** 보기 언어: "ko" 또는 "en" */
    @NotBlank
    private String meaningLang;

    /** 문제로 사용할 단어/뜻 */
    @NotNull
    @Valid
    private MemberVocabWordQuiz vocab;

    /** 문항 수 */
    @Min(5) @Max(30)
    private int count;
}
