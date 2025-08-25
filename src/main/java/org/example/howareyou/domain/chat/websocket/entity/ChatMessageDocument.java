package org.example.howareyou.domain.chat.websocket.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_message")
public class ChatMessageDocument {

  @Id
  private String id;

  @Indexed
  private String chatRoomUuid;

  @Indexed
  private String senderId;

  private String senderName;
  private String receiverId;
  private String receiverName;
  private String content;

  @Indexed
  private Instant messageTime;
  private ChatMessageStatus chatMessageStatus;
}
