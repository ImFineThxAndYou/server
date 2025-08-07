package org.example.howareyou.domain.notification.entity.payload;

import org.example.howareyou.domain.notification.entity.NotificationType;

public record SystemPayload(Long followerId, String message) implements NotificationPayload {
    @Override
    public NotificationType getType() {
        return NotificationType.SYSTEM;
    }
}