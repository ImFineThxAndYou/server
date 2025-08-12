package org.example.howareyou.domain.quiz.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter @Setter
public class DailyQuizRequest {
    @NotNull
    private Long memberId;

    /** yyyy-MM-dd */
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    /** 보기 언어: "ko" 또는 "en" */
    @NotBlank
    private String meaningLang;

    /** 문제로 사용할 단어/뜻 */
    @NotNull
    @Valid
    private String vocab;

    /** 문항수 */
    @Min(5) @Max(30) private int count;
}
