package org.example.howareyou.domain.quiz.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class DailyQuizRequest {
    @NotBlank
    private String memberName;

    @NotNull
    private LocalDate date;

    // 보기 언어를 덮어쓸 때만 보내는 선택값 ("ko" or "en")
    private String language;

    @Valid
    private RandomQuizRequest.MemberProfilePart memberProfile; // 선택
}
