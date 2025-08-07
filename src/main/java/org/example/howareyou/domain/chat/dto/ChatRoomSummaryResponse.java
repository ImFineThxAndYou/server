package org.example.howareyou.domain.chat.dto;

import java.time.Instant;

public record ChatRoomSummaryResponse(
    String chatRoomId,
    Long opponentId,
    String opponentName,
    String roomStatus,
    String lastMessageContent,
    Instant lastMessageTime,
    int unreadCount
) {}
