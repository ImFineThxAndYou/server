package org.example.howareyou.domain.quiz.dto.grade;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuizWordGrade {
    private final Long id;            // quiz_word_id
    private final Integer correctAnswer; // 정답번호 1-4
    // 보기
    private final String choice1;
    private final String choice2;
    private final String choice3;
    private final String choice4;

    // 현재 유효한 보기개수 (널값무시)
    public int choiceSize() {
        int n = 0;
        if (choice1 != null && !choice1.isBlank()) n++;
        if (choice2 != null && !choice2.isBlank()) n++;
        if (choice3 != null && !choice3.isBlank()) n++;
        if (choice4 != null && !choice4.isBlank()) n++;
        return n;
    }
}
