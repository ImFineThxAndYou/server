package org.example.howareyou.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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


    // 응시 시각
    @Column(name = "created_at")
    private Instant createdAt;


    /** 중복제출 방지 위해서 추가 */
    // 제출 완료 여부
    @Column(name = "completed")
    private Boolean completed;

    // 제출 완료 시각
    @Column(name = "completed_at")
    private Instant completedAt;

    // 퀴즈 상태
    @Enumerated(EnumType.STRING)
    @Column(name="quiz_status")
    private QuizStatus quizStatus; // 제출전/채점후 - PENDING/GRADE

    @OneToMany(mappedBy = "quizResult", cascade = CascadeType.ALL)
    @OrderBy("questionNo ASC") // 문항번호대로 정렬
    private List<QuizWord> quizWords = new ArrayList<>();

    /** 저장 직전에 기본값 보정 */
    @PrePersist
    private void prePersist() {
        if (this.uuid == null || this.uuid.isBlank()) {
            this.uuid = UUID.randomUUID().toString(); // 36자, length 40 컬럼에 충분
        }
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.completed == null) this.completed = Boolean.FALSE;
    }
}

