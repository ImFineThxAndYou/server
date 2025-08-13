package org.example.howareyou.domain.chat.websocket.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatPreviewDTO {
  private String chatRoomId;
  private String lastMessage;
  private int unreadCount;
  private String lastSender;
  private Instant time;
}
