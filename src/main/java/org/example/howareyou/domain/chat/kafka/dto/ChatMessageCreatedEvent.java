package org.example.howareyou.domain.chat.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessageCreatedEvent {
  private Long roomId;
  private String roomUuid;
  private Long senderId;
  private Long receiverId;
  private String messageId;
  private String content;



}
