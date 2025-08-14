package org.example.howareyou.domain.vocabulary.dto;

public record MessageItem(
        String messageId,
        String content,
        String senderId,
        String senderName,
        String messageTime
) {}
