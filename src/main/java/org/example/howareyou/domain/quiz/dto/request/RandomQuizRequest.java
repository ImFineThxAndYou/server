package org.example.howareyou.domain.quiz.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RandomQuizRequest {

    @NotBlank
    private String memberName;           // 필수

    // 보기 언어를 강제로 덮어쓸 때만 보내는 선택값 ("ko" or "en")
    private String language;

    // 예전 클라이언트 호환용(선택). 없으면 null일 수 있음.
    private MemberProfilePart memberProfile;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class MemberProfilePart {
        private String language;         // "ko" or "en"
    }
}
