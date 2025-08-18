package org.example.howareyou.domain.dashboard.dto;

import java.time.Instant;

public record ScorePoint(
        String quizUuid,
        Instant submittedAtUtc,
        Integer score // 0~100
) {}
