package org.example.howareyou.domain.chat.websocket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.websocket.dto.ChatEnterDTO;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.example.howareyou.domain.chat.websocket.service.ChatMessageService;
import org.example.howareyou.domain.chat.websocket.service.ChatRedisService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ChatController {

  private final SimpMessagingTemplate messagingTemplate;
  private final ChatMessageService chatMessageService;
  private final ChatRedisService chatRedisService;

  // 메시지 전송 엔드포인트: /app/chat.send
  @MessageMapping("/chat.send")
  public void sendMessage(@Payload ChatMessageDocument chatMessageDocument) {

    chatMessageService.saveChatMessage(chatMessageDocument); // 저장
    messagingTemplate.convertAndSend(
        "/topic/chatroom/" + chatMessageDocument.getChatRoomUuid(), chatMessageDocument
    );
  }

  @MessageMapping("/chat.enter")
  public void enterRoom(ChatEnterDTO dto) {
    String userId = dto.getUserId();
    String chatRoomId = dto.getChatRoomId();

    chatRedisService.setCurrentChatRoom(userId, chatRoomId);
    chatMessageService.markMessagesAsRead(chatRoomId, userId);

    log.info("유저 입장: userId={}, chatRoomId={}", userId, chatRoomId);
  }

}