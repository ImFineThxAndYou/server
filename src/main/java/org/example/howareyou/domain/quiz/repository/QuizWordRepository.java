package org.example.howareyou.domain.quiz.repository;

import org.example.howareyou.domain.quiz.dto.grade.QuizWordGrade;
import org.example.howareyou.domain.quiz.entity.QuizWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.*;
import java.util.List;

/**
 * 채점
 * */
public interface QuizWordRepository extends JpaRepository<QuizWord, Long> {

    /* 채점용 조회(보기-정답만)*/
    @Query("""
        select new org.example.howareyou.domain.quiz.dto.grade.QuizWordGrade(
            w.id, w.correctAnswer, w.choice1, w.choice2, w.choice3, w.choice4
        )
          from QuizWord w
         where w.quizResult.id = :quizResultId
         order by w.questionNo asc
    """)
    List<QuizWordGrade> findForGrading(@Param("quizResultId") Long quizResultId);

    /* 채점결과 반영 user answer 1~4, 미선택 null*/
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update QuizWord qw
           set qw.userAnswer = :userAnswer,
               qw.isCorrect  = :isCorrect
         where qw.id = :id
    """)
    int applyGrading(@Param("id") Long id,
                     @Param("userAnswer") Integer userAnswer,
                     @Param("isCorrect") Boolean isCorrect);

    /* 퀴즈 결과 ID로 퀴즈 단어 조회 (새로운 메서드) */
    List<QuizWord> findByQuizResultId(Long quizResultId);

    // 만약 바로 틀린 문제만 가져오고 싶다면 이렇게도 가능
    List<QuizWord> findByQuizResultIdAndIsCorrectFalse(Long quizResultId);
}
