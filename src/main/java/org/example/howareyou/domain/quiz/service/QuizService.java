package org.example.howareyou.domain.quiz.service;

import org.example.howareyou.domain.quiz.dto.response.QuizResultResponse;
import org.example.howareyou.domain.quiz.dto.submit.SubmitResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/* TODO : 퀴즈 생성되고, 퀴즈 풀다가 뒤로가기 눌렀을때 정책을 어떻게할까.

퀴즈 생성 POST , 문제를 따로 불러오는 GET 으로 API 만들고

*   다시 풀수있게?
* 퀴즈 상태 - 푸는중인지 완료됐는지 */
public interface QuizService {
    SubmitResponse gradeQuiz(String QuizeUUID, List<Integer> selectedIndices);

    // 회원별 퀴즈 전체조회
    @Transactional(readOnly = true)
    Page<QuizResultResponse> getQuizResultsByMember(Long memberId, Pageable pageable);

    // 회원별 퀴즈결과 상세조회
    @Transactional(readOnly = true)
    QuizResultResponse getQuizResultDetail(String quizUUID);

    // 날짜별 퀴즈결과
}
