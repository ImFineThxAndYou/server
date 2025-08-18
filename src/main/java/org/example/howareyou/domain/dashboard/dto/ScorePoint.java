package org.example.howareyou.domain.dashboard.dto;

import java.time.Instant;

public record ScorePoint(
        String quizUuid,//퀴즈 uuid : 추후에 해당 퀴즈로 이동 등등.
        Instant submittedAtUtc, // 제출시각
        Long score // 0~100
) {}
