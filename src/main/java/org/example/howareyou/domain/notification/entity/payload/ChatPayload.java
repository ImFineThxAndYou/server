package org.example.howareyou.domain.notification.entity.payload;

import org.example.howareyou.domain.notification.entity.NotificationType;

public record ChatPayload(Long chatRoomId, Long senderId, String preview) implements NotificationPayload {
    @Override
    public NotificationType getType() {
        return NotificationType.CHAT;
    }
}
