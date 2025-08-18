package org.example.howareyou.domain.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.dashboard.dto.ScorePoint;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.quiz.repository.QuizResultRepository;
import org.example.howareyou.domain.vocabulary.service.MemberVocaBookService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardServiceImpl implements DashboardService {

    private final MemberVocaBookService memberVocaBookService;
    private final MemberService memberService;
    private final QuizResultRepository quizResultRepository;

    // 단어장에있는 단어개수 count
    @Override
    public long countWords(String membername) {
        return memberVocaBookService.countLatestUniqueWordsTotal(membername);
    }

    // 연속 학습 날
    @Override
    public int getLearningDays(Long memberId, ZoneId zone) {
        // 기준일: 오늘
        LocalDate today = LocalDate.now(zone);
        // 최근 90일만 조회
        LocalDate from = today.minusDays(90);

        Instant fromUtc   = from.atStartOfDay(zone).toInstant();
        Instant toUtcExcl = today.plusDays(1).atStartOfDay(zone).toInstant();

        // 제출된(LocalDate) 날짜 집합
        List<Date> submittedDays = quizResultRepository
                .findDistinctSubmittedLocalDates(memberId, fromUtc, toUtcExcl, zone.getId());

        if (submittedDays.isEmpty()) return 0;

        // 오늘부터 끊길 때까지 역으로 카운트
        int streak = 0;
        LocalDate d = today;
        while (submittedDays.contains(d)) {
            streak++;
            d = d.minusDays(1);
        }
        return streak;
    }

    // 복습 필요날짜
    @Override
    public int countReviewDays(String membername, LocalDate from, LocalDate to, ZoneId zone) {
        Long memberId   = memberService.getIdByMembername(membername);
        Instant fromUtc = from.atStartOfDay(zone).toInstant();
        Instant toUtcExcl = to.plusDays(1).atStartOfDay(zone).toInstant();

        // 단어장 생성 날짜
        Set<LocalDate> vocabDays = memberVocaBookService.getDistinctVocabLocalDates(
                membername, fromUtc, toUtcExcl, zone.getId()
        );
        if (vocabDays.isEmpty()) return 0;

        // 퀴즈 생성 날짜
        List<Date> quizCreatedDays = quizResultRepository.findDistinctCreatedLocalDates(
                memberId, fromUtc, toUtcExcl, zone.getId()
        );

        // 복습 필요일 = 단어장은 있었는데 그 날 퀴즈는 생성되지 않음
        vocabDays.removeAll(quizCreatedDays);
        return vocabDays.size();
    }

    // 퀴즈 정답률
    @Override
    public List<ScorePoint> getQuizScoreSeries(Long memberId, Instant fromUtc, Instant toUtc, Integer limit) {
        int cap = (limit == null || limit <= 0) ? 1000 : limit;

        var rows = quizResultRepository.findAttempts(memberId, fromUtc, toUtc, cap);

        return rows.stream()
                .map(r -> new ScorePoint(
                        r.getUuid(),
                        r.getSubmittedAt(),                            // UTC (프론트에서 타임존 변환)
                        Objects.requireNonNullElse(r.getScore(), 0L)
                ))
                .collect(Collectors.toList());
    }

}
