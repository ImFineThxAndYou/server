package org.example.howareyou.domain.chat.websocket.dto;

import lombok.Builder;
import lombok.Getter;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;

import java.time.Instant;

@Getter
@Builder
public class ChatMessageDocumentResponse {
  private String id;

  private String chatRoomUuid;
  private String sender;
  private String content;
  private Instant messageTime;
  private String chatMessageStatus;

  public static ChatMessageDocumentResponse from(ChatMessageDocument chatMessageDocument) {
    return ChatMessageDocumentResponse.builder()
        .id(chatMessageDocument.getId())
        .sender(chatMessageDocument.getSender())
        .content(chatMessageDocument.getContent())
        .messageTime(chatMessageDocument.getMessageTime())
        .chatMessageStatus(chatMessageDocument.getChatMessageStatus().name())
        .build();
  }

}
