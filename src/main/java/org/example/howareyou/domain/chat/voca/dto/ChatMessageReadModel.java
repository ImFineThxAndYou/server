package org.example.howareyou.domain.chat.voca.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Builder
public class ChatMessageReadModel {
    private final String id;
    private final String chatRoomUuid;
    private final String sender;
    private final String content;
    private final Instant messageTime;
}