package org.example.howareyou.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ChatRoomSummaryResponse {
  private String uuid;
  private Long opponentId; // 상대방 ID
  private String opponentName; // 상대방 name
  private String status;

  @JsonIgnore
  private Instant messageTime; // 정렬용
}