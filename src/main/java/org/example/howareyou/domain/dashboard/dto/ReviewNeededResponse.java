package org.example.howareyou.domain.dashboard.dto;

import java.time.LocalDate;

public record ReviewNeededResponse(
        int reviewNeededDays,
        String encouragementMessage,
        LocalDate from,
        LocalDate to,
        String zone
) {}
