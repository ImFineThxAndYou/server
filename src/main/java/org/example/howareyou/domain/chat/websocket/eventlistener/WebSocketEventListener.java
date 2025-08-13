package org.example.howareyou.domain.chat.websocket.eventlistener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.websocket.service.ChatMemberTracker;
import org.example.howareyou.domain.chat.websocket.service.ChatRedisService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

  private final ChatMemberTracker chatMemberTracker;
  private final ChatRedisService chatRedisService;

//  @EventListener
//  public void handleWebSocketConnectListener(SessionConnectedEvent event) {
//    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
//
//    String userId = (String) headerAccessor.getSessionAttributes().get("userId");
//    String chatRoomId = (String) headerAccessor.getSessionAttributes().get("chatRoomId");
//
//    if (userId != null && chatRoomId != null) {
//      // 채팅방 접속 멤버 등록
//      chatMemberTracker.addUserToRoom(chatRoomId, userId);
//
//      // 유저가 현재 접속 중인 채팅방 ID Redis에 저장
//      chatRedisService.setCurrentChatRoom(userId, chatRoomId);
//
//      log.info("유저 입장: userId={}, chatRoomId={}", userId, chatRoomId);
//    }
//  }

  @EventListener
  public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

    String userId = (String) headerAccessor.getSessionAttributes().get("userId");
    String chatRoomId = (String) headerAccessor.getSessionAttributes().get("chatRoomId");

    if (userId != null && chatRoomId != null) {
      // 채팅방 접속 유저 목록에서 제거
      chatMemberTracker.removeUserFromRoom(chatRoomId, userId);

      // Redis에서 현재 접속 채팅방 정보 제거
      chatRedisService.clearCurrentChatRoom(userId);

      log.info("유저 퇴장: userId={}, chatRoomId={}", userId, chatRoomId);
    }
  }

}
