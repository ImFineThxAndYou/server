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
    /* 채점결과 반영 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update QuizWord w
           set w.userAnswer = :selectedIndex,
               w.isCorrect  = :isCorrect
         where w.id = :quizWordId
    """)
    int applyGrading(@Param("quizWordId") Long quizWordId,
                     @Param("selectedIndex") Integer selectedIndex,
                     @Param("isCorrect") Boolean isCorrect);


}
