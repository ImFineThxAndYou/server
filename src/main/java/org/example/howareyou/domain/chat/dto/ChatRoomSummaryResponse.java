package org.example.howareyou.domain.chat.dto;

import java.time.Instant;

public record ChatRoomSummaryResponse(
    String chatRoomId,
    Long opponentId,
    String opponentName,
    String roomStatus,
    String lastMessageContent,     // 추가
    Instant lastMessageTime,       // 추가
    int unreadCount                // 추가
) {}
