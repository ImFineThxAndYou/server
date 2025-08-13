package org.example.howareyou.domain.vocabulary.dto;

import java.util.List;

public record AnalyzeBatchRequest(
        String chatRoomUuid,
        List<MessageItem> messages
) {}


