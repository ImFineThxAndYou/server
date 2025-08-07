package org.example.howareyou.domain.chat.controller;

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.websocket.dto.ChatMessageDocumentResponse;
import org.example.howareyou.domain.chat.websocket.service.ChatMessageService;
import org.example.howareyou.global.security.CustomMemberDetails;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/chat-message")
public class ChatMessageController {

  private final ChatMessageService chatMessageService;

  /**
   * 최근 메시지 30개 조회 (Redis 캐시 우선)
   * @param chatRoomId 채팅방 ID
   */
  @GetMapping("/{chatRoomId}/recent")
  public List<ChatMessageDocumentResponse> getRecentMessages(
      @PathVariable String chatRoomId,
      @RequestParam(defaultValue = "30") int count
  ) {
    return chatMessageService.getRecentMessagesWithFallback(chatRoomId, count);
  }

  /**
   * 이전 메시지 페이징 조회 (무한스크롤)
   * @param chatRoomId 채팅방 ID
   * @param before 해당 시간 이전의 메시지
   * @param size 가져올 개수
   */
  @GetMapping("/{chatRoomId}/previous")
  public List<ChatMessageDocumentResponse> getPreviousMessages(
      @PathVariable String chatRoomId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
      @RequestParam(defaultValue = "30") int size
  ) {
    return chatMessageService.getPreviousMessages(chatRoomId, before, size);
  }

  /**
   * 메시지 읽음 처리
   * @param chatRoomId 채팅방 ID
   */
  @PostMapping("/{chatRoomId}/read")
  public void markMessagesAsRead(
      @PathVariable String chatRoomId,
      @AuthenticationPrincipal CustomMemberDetails memberDetails
  ) {
    String userId = memberDetails.getId().toString();
    chatMessageService.markMessagesAsRead(chatRoomId, userId);
  }

}
