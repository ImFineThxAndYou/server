package org.example.howareyou.domain.chat.websocket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.entity.ChatRoom;
import org.example.howareyou.domain.chat.repository.ChatRoomRepository;
import org.example.howareyou.domain.chat.websocket.dto.ChatEnterDTO;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.example.howareyou.domain.chat.websocket.service.ChatMessageService;
import org.example.howareyou.domain.chat.websocket.service.ChatRedisService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
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
  private final ChatRoomRepository chatRoomRepository;

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

    // 입장 제한 로직
    ChatRoom chatRoom = chatRoomRepository.findByUuid(chatRoomId)
        .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

    Long memberId = Long.parseLong(userId);

    // PENDING 상태 또는 본인이 멤버가 아니면 차단
    if (!chatRoom.hasParticipant(memberId)) {
      throw new CustomException(ErrorCode.FORBIDDEN_CHAT_ROOM_ACCESS);
    }

    // 입장 처리
    chatRedisService.setCurrentChatRoom(userId, chatRoomId);
    chatMessageService.markMessagesAsRead(chatRoomId, userId);

    log.info("유저 입장: userId={}, chatRoomId={}", userId, chatRoomId);
  }

}