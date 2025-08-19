package org.example.howareyou.domain.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.quiz.dto.ClientQuizQuestion;
import org.example.howareyou.domain.quiz.dto.ClientStartResponse;
import org.example.howareyou.domain.quiz.dto.response.QuizResultResponse;
import org.example.howareyou.domain.quiz.dto.response.WrongAnswerResponse;
import org.example.howareyou.domain.quiz.dto.submit.SubmitResponse;
import org.example.howareyou.domain.quiz.entity.QuizResult;
import org.example.howareyou.domain.quiz.entity.QuizStatus;
import org.example.howareyou.domain.quiz.entity.QuizWord;
import org.example.howareyou.domain.quiz.repository.QuizResultRepository;
import org.example.howareyou.domain.quiz.repository.QuizWordRepository;
import org.example.howareyou.domain.dashboard.dto.ScorePoint;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class QuizServiceImpl implements QuizService {

    private final QuizResultRepository quizResultRepository;
    private final QuizWordRepository quizWordRepository;

    /**
     * 퀴즈 채점 및 상태 업데이트
     * @param quizUuid   퀴즈 고유 식별자(UUID)
     * @param selected   사용자가 제출한 각 문항의 선택 번호 리스트 (1~4, 미선택은 -1)
     * @return 채점 결과(정답 개수, 총 문항 수, 점수 등)
     */
    @Override
    public SubmitResponse gradeQuiz(String quizUuid, List<Integer> selected) {
        // 1) 제출 여부 확인 (uuid 기반)
        boolean completed = quizResultRepository.findCompletedQuizByUuid(quizUuid)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_NOT_FOUND));
        if (completed) throw new CustomException(ErrorCode.QUIZ_ALREADY_SUBMITTED);

        // 2) 내부 PK(Long) 조회 (한 번만)
        Long quizResultId = quizResultRepository.findByUuid(quizUuid)
                .map(QuizResult::getId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_NOT_FOUND));

        // 3) 채점용 문항 경량 조회 (id 기반)
        var items = quizWordRepository.findForGrading(quizResultId);

        if (items.size() != selected.size()) {
            throw new CustomException(ErrorCode.INVALID_SELECTION_COUNT);
        }

        int correct = 0;
        /* 채점 */
        for (int i = 0; i < items.size(); i++) {
            var item = items.get(i);
            Integer selectedIndex = selected.get(i);

            if (Objects.equals(selectedIndex, item.getCorrectAnswer() - 1)) { // 0-based로 변환
                correct++;
            }
        }

        // 4) 점수 계산 및 상태 업데이트
        int totalQuestions = items.size();
        int score = (int) Math.round((double) correct / totalQuestions * 100);

        quizResultRepository.updateQuizResult(quizResultId, correct, totalQuestions, score, true);

        return SubmitResponse.builder()
                .correctCount(correct)
                .totalQuestions(totalQuestions)
                .score(score)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuizResultResponse> getQuizResultsByMember(Long memberId, QuizStatus status, Pageable pageable) {
        return quizResultRepository.findByMemberIdAndStatus(memberId, status, pageable)
                .map(QuizResultResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizResultResponse getQuizResultDetail(String quizUUID) {
        return quizResultRepository.findByUuid(quizUUID)
                .map(QuizResultResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientStartResponse getPendingQuizByUuid(Long memberId, String uuid) {
        QuizResult quizResult = quizResultRepository.findByUuidAndMemberId(uuid, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_NOT_FOUND));

        if (quizResult.getStatus() != QuizStatus.PENDING) {
            throw new CustomException(ErrorCode.QUIZ_ALREADY_SUBMITTED);
        }

        List<ClientQuizQuestion> questions = quizWordRepository.findByQuizResultId(quizResult.getId())
                .stream()
                .map(ClientQuizQuestion::from)
                .toList();

        return ClientStartResponse.builder()
                .quizResultId(quizResult.getId())
                .quizUUID(quizResult.getUuid())
                .quizQuestions(questions)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WrongAnswerResponse> getWrongAnswer(Long memberId) {
        return quizResultRepository.findLatestWrongAnswers(memberId);
    }

    /* -------------------- 대시보드용 메서드들 -------------------- */

    @Override
    @Transactional(readOnly = true)
    public int calculateLearningStreak(Long memberId, ZoneId zoneId, String period) {
        try {
            log.info("연속 학습일 계산 시작 - memberId: {}, zoneId: {}, period: {}", memberId, zoneId, period);
            
            LocalDate to = LocalDate.now(zoneId);
            LocalDate from = calculateFromDate(to, period);
            
            int streak = quizResultRepository.calculateLearningStreakByPeriod(memberId, from, to, zoneId.getId());
            
            log.info("연속 학습일 계산 완료 - memberId: {}, streak: {}", memberId, streak);
            return streak;
            
        } catch (Exception e) {
            log.error("연속 학습일 계산 실패 - memberId: {}, error: {}", memberId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countReviewNeededDays(Long memberId, LocalDate from, LocalDate to, ZoneId zoneId) {
        try {
            log.info("복습 필요 날짜 계산 시작 - memberId: {}, from: {}, to: {}", memberId, from, to);
            
            // 간단한 구현: 기간 내 퀴즈를 푼 날짜 수를 복습 필요 날짜로 계산
            int reviewDays = quizResultRepository.countQuizDaysByPeriod(memberId, from, to, zoneId.getId());
            
            log.info("복습 필요 날짜 계산 완료 - memberId: {}, reviewDays: {}", memberId, reviewDays);
            return reviewDays;
            
        } catch (Exception e) {
            log.error("복습 필요 날짜 계산 실패 - memberId: {}, error: {}", memberId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScorePoint> getScoreSeries(Long memberId, Instant fromUtc, Instant toUtc, Integer limit) {
        try {
            log.info("점수 시리즈 조회 시작 - memberId: {}, fromUtc: {}, toUtc: {}, limit: {}", memberId, fromUtc, toUtc, limit);
            
            List<ScorePoint> scoreSeries = quizResultRepository.findScoreSeriesByPeriod(memberId, fromUtc, toUtc)
                    .stream()
                    .limit(limit != null ? limit : 30) // 기본값 30개로 제한
                    .map(result -> new ScorePoint(
                        result.getUuid(),
                        result.getCompletedAt(),
                        result.getScore().intValue()
                    ))
                    .toList();
            
            log.info("점수 시리즈 조회 완료 - memberId: {}, count: {}", memberId, scoreSeries.size());
            return scoreSeries;
            
        } catch (Exception e) {
            log.error("점수 시리즈 조회 실패 - memberId: {}, error: {}", memberId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> getQuizGrass(Long memberId, int year, ZoneId zoneId, String period) {
        try {
            log.info("퀴즈 잔디 조회 시작 - memberId: {}, year: {}, zoneId: {}, period: {}", memberId, year, zoneId, period);
            
            LocalDate to = LocalDate.now(zoneId);
            LocalDate from = calculateFromDate(to, period);
            
            List<QuizResultRepository.DailyQuizCount> result = quizResultRepository.getDailyQuizCountsByPeriod(
                memberId, from, to, zoneId.getId());
            
            Map<String, Integer> grass = new HashMap<>();
            for (QuizResultRepository.DailyQuizCount daily : result) {
                grass.put(daily.getDate(), daily.getCount());
            }
            
            log.info("퀴즈 잔디 조회 완료 - memberId: {}, count: {}", memberId, grass.size());
            return grass;
            
        } catch (Exception e) {
            log.error("퀴즈 잔디 조회 실패 - memberId: {}, error: {}", memberId, e.getMessage(), e);
            return Map.of();
        }
    }

    /**
     * 기간에 따른 시작 날짜 계산
     */
    private LocalDate calculateFromDate(LocalDate to, String period) {
        return switch (period) {
            case "week" -> to.minusWeeks(1);
            case "month" -> to.minusMonths(1);
            default -> to.minusWeeks(1); // 기본값은 주간
        };
    }
}