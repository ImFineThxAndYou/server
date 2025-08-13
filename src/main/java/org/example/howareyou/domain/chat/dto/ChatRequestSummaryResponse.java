package org.example.howareyou.domain.chat.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatRequestSummaryResponse {
  private final String roomUuid;
  private final Long opponentId;
  private final String opponentName;
  private final String roomStatus;
  private final LocalDateTime createdAt;
}