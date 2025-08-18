package org.example.howareyou.domain.dashboard.service;

import org.example.howareyou.domain.dashboard.dto.ScorePoint;
import org.example.howareyou.domain.dashboard.dto.WrongAnswer;
import org.example.howareyou.domain.quiz.dto.response.WrongAnswerResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/*
* TODO :
*  1. 총 단어 개수 count - mongoDB 에서 member_vocabulary 에서  @AuthenticationPrincipal CustomMemberDetails member 에서 membername 추출 후 해당membername
*  2. 연속 학습일 count -  @AuthenticationPrincipal CustomMemberDetails member 에서 member_id 추출 후 quiz_result 에서 찾기
*  3. 복습 필요 날짜 count / 복습필요날짜 0 일 이면 "학습을 꾸준히 하시고계시는군요? 최고에요" 라는 문구 띄우기
*  4. 학습 성과분석 : 퀴즈 정답률 꺽은선 그래프 (quiz_result 의 score 을 꺽은선그래프) */
public interface DashboardService {
    // 단어장 - 멤버별 총 단어 개수
    long countWords(String membername, String lang, String pos);
    // 연속 학습일 - 프론트엔드에서 타임존 받아야해요
    int getLearningDays(Long memberId, ZoneId zone);
    // 복습 필요한 날짜
    int countReviewDays(Long memberId, LocalDate from, LocalDate to, ZoneId zone);
    // 학습 성과 분석
    List<ScorePoint> getQuizScoreSeries(Long memberId, Instant fromUtc, Instant toUtc, Integer limit);
    //오답노트
    List<WrongAnswer> getWrongAnswer(Long memberId);




}
