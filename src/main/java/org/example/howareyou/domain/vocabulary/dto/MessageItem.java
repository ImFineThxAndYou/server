package org.example.howareyou.domain.vocabulary.dto;

public record MessageItem(
        String messageId,
        String content,
        String sender,        // 옵션: 미리 언어 아는 경우
        String messageTime
) {}
