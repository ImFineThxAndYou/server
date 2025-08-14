package org.example.howareyou.domain.chat.websocket.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@Document(collection = "chat_message")
public class ChatMessageDocument {

  @Id
  private String id;

  private String chatRoomUuid;
  private String senderId;
  private String senderName;
  private String receiverId;
  private String receiverName;
  private String content;
  private Instant messageTime;
  private ChatMessageStatus chatMessageStatus;
}
