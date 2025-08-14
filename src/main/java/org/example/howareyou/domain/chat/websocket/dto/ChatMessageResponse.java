package org.example.howareyou.domain.chat.websocket.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageStatus;

@Getter
@AllArgsConstructor(staticName = "from")
public class ChatMessageResponse {
  private String id;
  private String chatRoomUuid;
  private String senderId;
  private String senderName;
  private String receiverId;
  private String receiverName;
  private String content;
  private Instant messageTime;
  private ChatMessageStatus status;

  public static ChatMessageResponse from(ChatMessageDocument d) {
    return new ChatMessageResponse(
        d.getId(), d.getChatRoomUuid(), d.getSenderId(), d.getSenderName(),
        d.getReceiverId(), d.getReceiverName(), d.getContent(),
        d.getMessageTime(), d.getChatMessageStatus()
    );
  }
}

