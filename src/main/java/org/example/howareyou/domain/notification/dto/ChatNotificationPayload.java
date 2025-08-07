package org.example.howareyou.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatNotificationPayload {
    private Long chatRoomId;
    private Long senderId;
    private String preview;
}