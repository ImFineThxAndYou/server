package org.example.howareyou.domain.quiz.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.example.howareyou.domain.quiz.entity.QuizLevel;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RandomQuizRequest {
    // 보기 언어를 강제로 덮어쓸 때만 보내는 선택값 ("ko" or "en")
    private String language;
    private MemberProfilePart memberProfile;
    private QuizLevel quizLevel; // nullable A/B/C

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class MemberProfilePart {
        private String language;         // "ko" or "en"
    }
}
