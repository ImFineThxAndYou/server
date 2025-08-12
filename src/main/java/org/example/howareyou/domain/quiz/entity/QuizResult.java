package org.example.howareyou.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_result")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class QuizResult {

    @Id // 퀴즈ID
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_result_id")
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 40)
    private String uuid;

    // 유저id
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 원본퀴즈id (재응시일경우 필요)
    @Column(name = "original_quiz_id",nullable = true)
    private Long originalQuizId;

    // 퀴즈 타입 'daily' or 'random'
    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_type")
    private QuizType quizType;

    // 점수
    @Column(name="score")
    private Long score;

    //총 문제수
    @Column(name = "total_questions")
    private Long totalQuestions;

    // 맞힌 문제수
    @Column(name = "correct_count")
    private Long correctCount;

    // 날짜별 퀴즈 일때 들어가는 날짜
    @Column(name = "daily_quiz")
    private Instant dailyQuiz;

    // 재응시여부 기본 f
    @Column(name = "is_requiz")
    private Boolean isRequiz;

    // 응시 시각
    @Column(name = "created_at")
    private Instant createdAt;

    // 재응시 횟수 - 최대 5번
    @Column(name = "quiz_count")
    private int quiz_count;

    /** 중복제출 방지 위해서 추가 */
    // 제출 완료 여부
    @Column(name = "completed")
    private Boolean completed;

    // 제출 완료 시각
    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "quizResult", cascade = CascadeType.ALL)
    @OrderBy("questionNo ASC") // 문항번호대로 정렬
    private List<QuizWord> quizWords = new ArrayList<>();
}

