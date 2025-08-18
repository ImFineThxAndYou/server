package org.example.howareyou.domain.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.dashboard.dto.ScorePoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardServiceImpl implements DashboardService {

    @Override
    public long countWords(String membername, String lang, String pos) {
        return 0;
    }

    @Override
    public int getLearningDays(Long memberId, ZoneId zone) {
        return 0;
    }

    @Override
    public int countReviewDays(Long memberId, LocalDate from, LocalDate to, ZoneId zone) {
        return 0;
    }

    @Override
    public List<ScorePoint> getQuizScoreSeries(Long memberId, Instant fromUtc, Instant toUtc, Integer limit) {
        return List.of();
    }
}
