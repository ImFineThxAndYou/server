package org.example.howareyou.domain.notification.dto;

import org.example.howareyou.domain.notification.entity.NotificationType;

import java.time.Instant;

public record NotifyDto(
        String id,
        NotificationType type,
        Instant createdAt,
        Instant readAt,
        String payload           // 필요하면 클래스로 파싱
) {}

