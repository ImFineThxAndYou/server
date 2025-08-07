package org.example.howareyou.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatRoomSummaryResponse {
  private String uuid;
  private Long opponentId; // 상대방 ID
  private String opponentName; // 상대방 name
  private String status;
}