package org.example.howareyou.domain.quiz.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 *  QuizWord : 퀴즈단어 (단어레벨, 원문,품사,뜻 보기 1,2,3, 원본단어장 id, 정답번호 등등 저장
 */
@Getter
@Builder
public class QuizWordCreate {
    private final String word;          // 질문에 사용된 단어
    private final List<String> choices; // 보기(4개)
    private final int answerIndex;      // 사용자가 답한 index 0~3
    private final Integer questionNo;   // 1..N
    private final String meaning;       // 정답 의미(choices.get(answerIndex))
    private final String pos; // 품사
    private final String level; // 레벨
    private final String vocabOriginId; // 단어장 원본id
}
