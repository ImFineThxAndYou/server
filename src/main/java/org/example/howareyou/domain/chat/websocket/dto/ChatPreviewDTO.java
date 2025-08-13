package org.example.howareyou.domain.chat.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
