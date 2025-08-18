package org.example.howareyou.domain.chat.websocket.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;

@Getter
@Builder
public class ChatMessageDocumentResponse {
  private String id;

  private String chatRoomUuid;
  private String senderName;
  private String content;
  private Instant messageTime;
  private String chatMessageStatus;

  public static ChatMessageDocumentResponse from(ChatMessageDocument chatMessageDocument) {
    return ChatMessageDocumentResponse.builder()
        .id(chatMessageDocument.getId())
        .chatRoomUuid(chatMessageDocument.getChatRoomUuid())
        .senderName(chatMessageDocument.getSenderName())
        .content(chatMessageDocument.getContent())
        .messageTime(chatMessageDocument.getMessageTime())
        .chatMessageStatus(chatMessageDocument.getChatMessageStatus().name())
        .build();
  }

}
