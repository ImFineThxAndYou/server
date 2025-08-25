package org.example.howareyou.domain.chat.websocket.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageStatus;

@Getter
@AllArgsConstructor // ✅ staticName 제거
@Builder
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

  /** Mongo Document -> Response 변환 */
  public static ChatMessageResponse from(ChatMessageDocument d) {
    return new ChatMessageResponse(
        d.getId(),
        d.getChatRoomUuid(),
        d.getSenderId(),
        d.getSenderName(),
        d.getReceiverId(),
        d.getReceiverName(),
        d.getContent(),
        d.getMessageTime(),
        d.getChatMessageStatus()
    );
  }

  /** 임시 응답 DTO 생성용 (Mongo 저장 전에 사용 가능) */
  public static ChatMessageResponse of(
      String id,
      String chatRoomUuid,
      String senderId,
      String senderName,
      String receiverId,
      String receiverName,
      String content,
      Instant messageTime,
      ChatMessageStatus status
  ) {
    return new ChatMessageResponse(
        id, chatRoomUuid, senderId, senderName,
        receiverId, receiverName, content, messageTime, status
    );
  }
}
