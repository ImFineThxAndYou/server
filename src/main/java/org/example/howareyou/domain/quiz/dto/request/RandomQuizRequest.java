package org.example.howareyou.domain.quiz.dto.request;

import lombok.*;
import org.example.howareyou.domain.quiz.entity.QuizLevel;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RandomQuizRequest {
    private MemberProfilePart memberProfile;
    private QuizLevel quizLevel; // nullable A/B/C

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class MemberProfilePart {
        private String language;         // "ko" or "en"
    }
}
