package org.example.howareyou.domain.chat.voca.dto;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Builder
public class ChatMessageReadModel {
    @Id
    private final String id;
    private final String chatRoomUuid;
    private final String sender;
    private final String content;
    private final Instant messageTime;
}