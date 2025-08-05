package org.example.howareyou.domain.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRoomResponse {
  private String uuid;
  private String roomStatus;
  private Long memberId;
  private String memberName;

  public ChatRoomResponse(String uuid, String roomStatus, Long memberId, String memberName) {
    this.uuid = uuid;
    this.roomStatus = roomStatus;
    this.memberId = memberId;
    this.memberName = memberName;
  }


}
