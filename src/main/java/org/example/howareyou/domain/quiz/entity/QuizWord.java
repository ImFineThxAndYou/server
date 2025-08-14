package org.example.howareyou.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "quiz_word")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizWord {

    @Id // 퀴즈 단어 id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_word_id")
    private Long id;

    // 퀴즈 결과 id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_result")
    private QuizResult quizResult;

    // 단어 레벨 A1~C1 (C1가 가장어려움), 한국어는 A,B,C
    @Enumerated(EnumType.STRING)
    private QuizLevel level;

    // 단어 원문
    private String word;

    // 단어 품사
    private String pos;

    // 단어 뜻
    private String meaning;


    // 문항 번호
    @Column(name = "question_no", nullable = false)
    private Integer questionNo;

    // 보기1번
    @Column(name = "choice_1")
    private String choice1;

    // 보기2번
    @Column(name = "choice_2")
    private String choice2;

    // 보기3번
    @Column(name = "choice_3")
    private String choice3;
    // 보기4번
    @Column(name = "choice_4")
    private String choice4;

    // 정답 번호
    @Column(name = "correct_answer")
    private Integer correctAnswer; // 1 or 2

    // 유저 선택
    @Column(name = "user_answer")
    private Integer userAnswer;

    // 정답여부
    @Column(name = "is_correct")
    private Boolean isCorrect;

    // 생성시간
    @Column(name = "created_at")
    private Instant createdAt;
}

