package org.example.howareyou.domain.chat.websocket.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
public class CreateChatMessageRequest {

  private String chatRoomUuid;
  private Long senderId;
  private String content;

}
