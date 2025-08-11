package org.example.howareyou.domain.notification.entity.payload;

import org.example.howareyou.domain.notification.entity.NotificationType;

public interface NotificationPayload {
    NotificationType getType();
}
