package org.example.howareyou.domain.chat.websocket.controller;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatMessageController {

  private final SimpMessagingTemplate messagingTemplate;

  // 메시지 전송 엔드포인트: /app/chat.send
  @MessageMapping("/chat.send")
  public void sendMessage(@Payload ChatMessageDocument chatMessageDocument) {

    messagingTemplate.convertAndSend(
        "/topic/chatroom/" + chatMessageDocument.getChatRoomUuid(), chatMessageDocument
    );
  }
}