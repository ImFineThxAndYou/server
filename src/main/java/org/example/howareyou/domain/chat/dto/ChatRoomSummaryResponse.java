package org.example.howareyou.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatRoomSummaryResponse {
  private String uuid;
  private Long opponentId;
  private String opponentName;
  private String status;
}