package org.example.howareyou.domain.chat.websocket.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@Jacksonized
@Document(collection = "chat_message")
public class ChatMessageDocument {

  @Id
  private String id;

  private String chatRoomUuid;
  private String sender;
  private String content;
  private Instant messageTime;
  private ChatMessageStatus chatMessageStatus;
}
