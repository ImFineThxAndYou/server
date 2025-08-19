package org.example.howareyou.domain.dashboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.dashboard.dto.DashboardSummary;
import org.example.howareyou.domain.dashboard.dto.ScorePoint;
import org.example.howareyou.domain.dashboard.dto.WrongAnswer;
import org.example.howareyou.domain.quiz.dto.response.WrongAnswerResponse;
import org.example.howareyou.domain.quiz.service.QuizService;
import org.example.howareyou.domain.vocabulary.service.MemberVocaBookService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final QuizService quizService;
    private final MemberVocaBookService memberVocaBookService;

    @Override
    public DashboardSummary getDashboardSummary(Long memberId, ZoneId zoneId, String period) {
        try {
            log.info("대시보드 요약 정보 조회 시작 - memberId: {}, zoneId: {}, period: {}", memberId, zoneId, period);
            
            // 기간에 따른 날짜 범위 계산
            LocalDate to = LocalDate.now(zoneId);
            LocalDate from = calculateFromDate(to, period);
            
            // 1. 총 단어 개수 조회 (전체, 기간별 필터링 없음)
            long totalWords = memberVocaBookService.countTotalWordsByMemberId(memberId);
            
            // 2. 연속 학습일 계산 (기간별 필터링)
            int learningStreakDays = quizService.calculateLearningStreak(memberId, zoneId, period);
            
            // 3. 복습 필요 날짜 계산 (기간별)
            int reviewNeededDays = quizService.countReviewNeededDays(memberId, from, to, zoneId);
            
            // 4. 격려 메시지 생성
            String encouragementMessage = reviewNeededDays == 0 
                ? "학습을 꾸준히 하시고계시는군요? 최고에요!" 
                : "오늘도 열심히 학습해보세요!";
            
            // 5. 학습 성과 분석 (기간별)
            Instant toUtc = Instant.now();
            Instant fromUtc = from.atStartOfDay(zoneId).toInstant();
            int limit = "week".equals(period) ? 7 : 30; // 주간: 7일, 월간: 30일
            List<ScorePoint> scoreSeries = quizService.getScoreSeries(memberId, fromUtc, toUtc, limit);
            
            // 6. 오답노트 조회 (최근 10개) - 일단 빈 리스트로 처리
            List<WrongAnswerResponse> wrongAnswerNotes = quizService.getWrongAnswer(memberId);
            
            DashboardSummary summary = new DashboardSummary(
                totalWords,
                learningStreakDays,
                reviewNeededDays,
                encouragementMessage,
                scoreSeries,
                wrongAnswerNotes
            );
            
            log.info("대시보드 요약 정보 조회 완료 - memberId: {}", memberId);
            return summary;
            
        } catch (Exception e) {
            log.error("대시보드 요약 정보 조회 실패 - memberId: {}, error: {}", memberId, e.getMessage(), e);
            throw new CustomException(ErrorCode.DASHBOARD_CALCULATION_FAILED, "대시보드 데이터 계산 중 오류가 발생했습니다.");
        }
    }

    @Override
    public long countWords(Long memberId, String lang, String pos, String period) {
        try {
            log.info("단어 개수 조회 시작 - memberId: {}, lang: {}, pos: {}, period: {}", memberId, lang, pos, period);
            
            // 기간에 따른 날짜 범위 계산
            LocalDate to = LocalDate.now();
            LocalDate from = calculateFromDate(to, period);
            
            long count;
            if (lang != null && pos != null) {
                count = memberVocaBookService.countByMemberAndLangAndPosAndPeriod(memberId, lang, pos, from, to);
            } else if (lang != null) {
                count = memberVocaBookService.countByMemberAndLangAndPeriod(memberId, lang, from, to);
            } else if (pos != null) {
                count = memberVocaBookService.countByMemberAndPosAndPeriod(memberId, pos, from, to);
            } else {
                count = memberVocaBookService.countByMemberIdAndPeriod(memberId, from, to);
            }
            
            log.info("단어 개수 조회 완료 - memberId: {}, count: {}", memberId, count);
            return count;
            
        } catch (Exception e) {
            log.error("단어 개수 조회 실패 - memberId: {}, error: {}", memberId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int getLearningDays(Long memberId, ZoneId zoneId, String period) {
        try {
            log.info("연속 학습일 조회 시작 - memberId: {}, zoneId: {}, period: {}", memberId, zoneId, period);
            
            int learningDays = quizService.calculateLearningStreak(memberId, zoneId, period);
            
            log.info("연속 학습일 조회 완료 - memberId: {}, learningDays: {}", memberId, learningDays);
            return learningDays;
            
        } catch (Exception e) {
            log.error("연속 학습일 조회 실패 - memberId: {}, error: {}", memberId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int countReviewDays(Long memberId, LocalDate from, LocalDate to, ZoneId zoneId) {
        try {
            log.info("복습 필요 날짜 조회 시작 - memberId: {}, from: {}, to: {}", memberId, from, to);
            
            int reviewDays = quizService.countReviewNeededDays(memberId, from, to, zoneId);
            
            log.info("복습 필요 날짜 조회 완료 - memberId: {}, reviewDays: {}", memberId, reviewDays);
            return reviewDays;
            
        } catch (Exception e) {
            log.error("복습 필요 날짜 조회 실패 - memberId: {}, error: {}", memberId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public List<ScorePoint> getQuizScoreSeries(Long memberId, Instant fromUtc, Instant toUtc, Integer limit) {
        try {
            log.info("퀴즈 점수 시리즈 조회 시작 - memberId: {}, fromUtc: {}, toUtc: {}, limit: {}", memberId, fromUtc, toUtc, limit);
            
            List<ScorePoint> scoreSeries = quizService.getScoreSeries(memberId, fromUtc, toUtc, limit);
            
            log.info("퀴즈 점수 시리즈 조회 완료 - memberId: {}, count: {}", memberId, scoreSeries.size());
            return scoreSeries;
            
        } catch (Exception e) {
            log.error("퀴즈 점수 시리즈 조회 실패 - memberId: {}, error: {}", memberId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<WrongAnswer> getWrongAnswer(Long memberId) {
        try {
            log.info("오답노트 조회 시작 - memberId: {}", memberId);
            
            List<WrongAnswer> wrongAnswers = quizService.getWrongAnswer(memberId).stream()
                .map(r -> new WrongAnswer(r.getWord(), r.getMeaning(), r.getPos()))
                .toList();
            
            log.info("오답노트 조회 완료 - memberId: {}, count: {}", memberId, wrongAnswers.size());
            return wrongAnswers;
            
        } catch (Exception e) {
            log.error("오답노트 조회 실패 - memberId: {}, error: {}", memberId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Integer> getLearningGrass(Long memberId, int year, ZoneId zoneId, String period) {
        try {
            log.info("학습 잔디 조회 시작 - memberId: {}, year: {}, zoneId: {}, period: {}", memberId, year, zoneId, period);
            
            Map<String, Integer> grass = memberVocaBookService.getLearningGrass(memberId, year, zoneId, period);
            
            log.info("학습 잔디 조회 완료 - memberId: {}, count: {}", memberId, grass.size());
            return grass;
            
        } catch (Exception e) {
            log.error("학습 잔디 조회 실패 - memberId: {}, error: {}", memberId, e.getMessage(), e);
            return Map.of();
        }
    }

    @Override
    public Map<String, Integer> getVocabularyGrass(Long memberId, int year, ZoneId zoneId, String period) {
        try {
            log.info("단어장 잔디 조회 시작 - memberId: {}, year: {}, zoneId: {}, period: {}", memberId, year, zoneId, period);
            
            Map<String, Integer> grass = memberVocaBookService.getVocabularyGrass(memberId, year, zoneId, period);
            
            log.info("단어장 잔디 조회 완료 - memberId: {}, count: {}", memberId, grass.size());
            return grass;
            
        } catch (Exception e) {
            log.error("단어장 잔디 조회 실패 - memberId: {}, error: {}", memberId, e.getMessage(), e);
            return Map.of();
        }
    }

    @Override
    public Map<String, Integer> getQuizGrass(Long memberId, int year, ZoneId zoneId, String period) {
        try {
            log.info("퀴즈 잔디 조회 시작 - memberId: {}, year: {}, zoneId: {}, period: {}", memberId, year, zoneId, period);
            
            Map<String, Integer> grass = quizService.getQuizGrass(memberId, year, zoneId, period);
            
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
