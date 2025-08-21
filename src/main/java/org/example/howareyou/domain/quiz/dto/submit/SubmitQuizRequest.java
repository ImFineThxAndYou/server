package org.example.howareyou.domain.quiz.dto.submit;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter@Setter
public class SubmitQuizRequest {
    // 제출하는 퀴즈 id
    private Long quizResultId;
//    // 사용자가 선택한 답안 index
//    @Size(min=1)
//    private List<Integer> selectedIndexes;

    // 사용자가 선택한 답안 index (1-based: 1~4)
    @NotEmpty(message = "선택지는 비어 있을 수 없습니다.")
    @Size(min = 1, message = "선택지는 1개 이상이어야 합니다.")
    @JsonAlias({"selected"})   // 클라이언트에서 selected 로 보내도 매핑됨
    private List<Integer> selected;

    // record 스타일처럼 쓰고 싶다면 getter 메서드 추가
    public List<Integer> selected() {
        return selected;
    }
}
