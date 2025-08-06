package org.example.howareyou.domain.chat.websocket.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatNotificationDTO {
  private String chatRoomId;
  private String senderId;
  private String content;
  private LocalDateTime messageTime;
}
