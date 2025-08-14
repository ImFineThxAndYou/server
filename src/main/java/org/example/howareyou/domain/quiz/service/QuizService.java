package org.example.howareyou.domain.quiz.service;

import org.example.howareyou.domain.quiz.dto.ClientStartResponse;
import org.example.howareyou.domain.quiz.dto.response.QuizResultResponse;
import org.example.howareyou.domain.quiz.dto.submit.SubmitResponse;
import org.example.howareyou.domain.quiz.entity.QuizStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface QuizService {
    // 채점
    SubmitResponse gradeQuiz(String QuizeUUID, List<Integer> selectedIndices);

    // 회원별 퀴즈 전체조회
    @Transactional(readOnly = true)
    Page<QuizResultResponse> getQuizResultsByMember(Long memberId, QuizStatus status, Pageable pageable);

    // 회원별 퀴즈결과 상세조회
    QuizResultResponse getQuizResultDetail(String quizUUID);


}
