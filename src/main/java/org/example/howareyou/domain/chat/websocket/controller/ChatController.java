package org.example.howareyou.domain.chat.websocket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.howareyou.domain.chat.entity.ChatRoom;
import org.example.howareyou.domain.chat.repository.ChatRoomRepository;
import org.example.howareyou.domain.chat.websocket.dto.ChatEnterDTO;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.example.howareyou.domain.chat.websocket.service.ChatMemberTracker;
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
@Tag(name = "WebSocket 채팅", description = "STOMP 기반 실시간 채팅 메시지 처리")
public class ChatController {

  private final SimpMessagingTemplate messagingTemplate;
  private final ChatMessageService chatMessageService;
  private final ChatRedisService chatRedisService;
  private final ChatRoomRepository chatRoomRepository;
  private final ChatMemberTracker chatMemberTracker;

  /**
   * 채팅 메시지 송신 (WebSocket STOMP)
   */
  @Operation(
      summary = "채팅 메시지 전송",
      description = """
      클라이언트가 WebSocket으로 /app/chat.send에 메시지를 전송하면,
      서버는 메시지를 저장하고 /topic/chatroom/{chatRoomId}로 브로드캐스팅합니다.
      """
  )
  @MessageMapping("/chat.send")
  public void sendMessage(
      @Parameter(description = "채팅 메시지 문서", required = true)
      @Payload ChatMessageDocument chatMessageDocument
  ) {
    chatMessageService.saveChatMessage(chatMessageDocument); // 저장
    messagingTemplate.convertAndSend(
        "/topic/chatroom/" + chatMessageDocument.getChatRoomUuid(), chatMessageDocument
    );
  }

  /**
   * 채팅방 입장 처리 (WebSocket STOMP)
   */
  @Operation(
      summary = "채팅방 입장 처리",
      description = """
      사용자가 채팅방에 입장할 때 /app/chat.enter로 입장 이벤트를 보냅니다.
      서버는 사용자의 입장 권한을 확인하고 Redis에 입장 상태를 저장한 후,
      기존 메시지를 읽음 처리합니다.
      """
  )
  @MessageMapping("/chat.enter")
  public void enterRoom(
      @Parameter(description = "입장 DTO (userId, chatRoomId 포함)", required = true)
      @Payload ChatEnterDTO dto
  ) {
    String userId = dto.getUserId();
    String chatRoomId = dto.getChatRoomId();

    // 입장 제한 로직
    ChatRoom chatRoom = chatRoomRepository.findByUuid(chatRoomId)
        .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

    Long memberId = Long.parseLong(userId);

    if (!chatRoom.hasParticipant(memberId)) {
      throw new CustomException(ErrorCode.FORBIDDEN_CHAT_ROOM_ACCESS);
    }

    // 채팅방 접속 멤버 등록
    chatMemberTracker.addUserToRoom(chatRoomId, userId);

    // 유저가 현재 접속 중인 채팅방 ID Redis에 저장
    chatRedisService.setCurrentChatRoom(userId, chatRoomId);

    // 채팅방 읽음 처리
    chatMessageService.markMessagesAsRead(chatRoomId, userId);

    log.info("유저 입장: userId={}, chatRoomId={}", userId, chatRoomId);
  }
}
