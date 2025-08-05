package org.example.howareyou.domain.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateChatRoomResponse {

  private String uuid;

  public CreateChatRoomResponse(String uuid) {
    this.uuid = uuid;
  }
}
