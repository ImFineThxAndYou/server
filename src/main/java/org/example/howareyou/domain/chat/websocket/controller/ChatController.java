package org.example.howareyou.domain.chat.websocket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.howareyou.domain.chat.entity.ChatRoom;
import org.example.howareyou.domain.chat.repository.ChatRoomRepository;
import org.example.howareyou.domain.chat.websocket.dto.ChatEnterDTO;
import org.example.howareyou.domain.chat.websocket.dto.ChatMessageResponse;
import org.example.howareyou.domain.chat.websocket.dto.CreateChatMessageRequest;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.example.howareyou.domain.chat.websocket.service.ChatMemberTracker;
import org.example.howareyou.domain.chat.websocket.service.ChatMessageService;
import org.example.howareyou.domain.chat.websocket.service.ChatRedisService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;

import org.example.howareyou.global.security.CustomMemberDetails;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
  @MessageMapping("/chat.send") // 클라는 /app/chat.send 로 send
  public void sendMessage(
      @Valid @Payload CreateChatMessageRequest req,
      SimpMessageHeaderAccessor headerAccessor
  ) {
    log.info("🚀 ChatController.sendMessage 호출됨!");
    log.info("📋 Request 정보: {}", req);
    
    String contentPreview = req.getContent() != null && req.getContent().length() > 50 
        ? req.getContent().substring(0, 50) + "..." 
        : req.getContent();
    log.info("📨 채팅 메시지 수신: chatRoomUuid={}, content={}", 
             req.getChatRoomUuid(), contentPreview);
    
    // headerAccessor에서 인증 정보 추출
    Authentication auth = (Authentication) headerAccessor.getUser();
    CustomMemberDetails member = null;
    
    if (auth != null && auth.getPrincipal() instanceof CustomMemberDetails) {
      member = (CustomMemberDetails) auth.getPrincipal();
      log.info("👤 인증된 사용자: ID={}, membername={}", member.getId(), member.getMembername());
      
      // senderId가 null이면 인증된 사용자 ID로 설정
      if (req.getSenderId() == null) {
        req.setSenderId(member.getId());
        log.info("✅ senderId를 인증된 사용자 ID로 설정: {}", member.getId());
      } else if (!req.getSenderId().equals(member.getId())) {
        log.error("❌ Sender ID 불일치: req.senderId={}, authMemberId={}", req.getSenderId(), member.getId());
        throw new CustomException(ErrorCode.FORBIDDEN_CHAT_ROOM_ACCESS, "Sender mismatch");
      }
    } else {
      log.warn("⚠️ 인증된 사용자 정보가 없음 - 임시로 테스트 ID 사용");
      req.setSenderId(1L); // 임시 테스트용 ID
    }

    try {
      // 서비스는 Document 반환(저장된 값 포함: id, 시간 등)
      ChatMessageResponse savedDoc = chatMessageService.saveChatMessage(req);
      log.info("💾 메시지 저장 성공: messageId={}", savedDoc.getId());

      // 브로드캐스트는 DTO로 (스키마 결합 방지)
      String destination = "/topic/chatroom/" + savedDoc.getChatRoomUuid();
      messagingTemplate.convertAndSend(destination, savedDoc);
      log.info("📢 메시지 브로드캐스트 완료: destination={}", destination);
      
    } catch (Exception e) {
      log.error("❌ 메시지 처리 중 오류 발생", e);
      throw e;
    }
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
      @Payload ChatEnterDTO dto,
      SimpMessageHeaderAccessor headerAccessor
  ) {
    log.info("🚀 ChatController.enterRoom 호출됨!");
    log.info("📋 Enter DTO: {}", dto);
    
    // headerAccessor에서 인증 정보 추출
    Authentication auth = (Authentication) headerAccessor.getUser();
    CustomMemberDetails memberDetails = null;
    String userId;
    
    if (auth != null && auth.getPrincipal() instanceof CustomMemberDetails) {
      memberDetails = (CustomMemberDetails) auth.getPrincipal();
      userId = memberDetails.getId().toString();
      log.info("👤 인증된 사용자: ID={}, membername={}", memberDetails.getId(), memberDetails.getMembername());
    } else {
      log.warn("⚠️ 인증된 사용자 정보가 없음 - 임시로 테스트 ID 사용");
      userId = "5"; // 임시 테스트용 ID
    }
    
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
