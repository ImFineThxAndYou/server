package org.example.howareyou.domain.quiz.dto.dictionary;

import lombok.*;
/**
 * 사용자 퀴즈 오답풀 미리 생성
 * */
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistractorDTO {
    // 정답단어
    private String word;
    //퀴즈 레벨
    private String level;
    //품사
    private String pos;
    //오답후보 리스트
    private List<String> distractor;

}
