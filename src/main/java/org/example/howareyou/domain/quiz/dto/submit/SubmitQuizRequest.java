package org.example.howareyou.domain.quiz.dto.submit;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter@Setter
public class SubmitQuizRequest {
    // 제출하는 퀴즈 id
    private Long quizResultId;
    // 사용자가 선택한 답안 index
    @Size(min=1)
    private List<Integer> selectedIndexes;
}
