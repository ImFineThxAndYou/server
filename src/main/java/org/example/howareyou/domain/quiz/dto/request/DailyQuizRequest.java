package org.example.howareyou.domain.quiz.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.example.howareyou.domain.quiz.entity.QuizLevel;

import java.time.LocalDate;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class DailyQuizRequest {
    @NotBlank
    private String membername;

    @NotNull
    private String date; //yyyy-mm-dd 형식으로 받자

    // 보기 언어를 덮어쓸 때만 보내는 선택값 ("ko" or "en")
    private String language;
    //퀴즈레벨 A-B-C
    private QuizLevel quizLevel;

    @Valid
    private RandomQuizRequest.MemberProfilePart memberProfile; // 선택

}
