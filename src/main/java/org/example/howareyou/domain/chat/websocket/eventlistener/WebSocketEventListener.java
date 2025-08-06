package org.example.howareyou.domain.chat.websocket.eventlistener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.websocket.service.ChatMemberTracker;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

  private final ChatMemberTracker chatMemberTracker;

  @EventListener
  public void handleWebSocketConnectListener(SessionConnectedEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

    String userId = (String) headerAccessor.getSessionAttributes().get("userId");
    String chatRoomId = (String) headerAccessor.getSessionAttributes().get("chatRoomId");

    if (userId != null && chatRoomId != null) {
      chatMemberTracker.addUserToRoom(chatRoomId, userId);
      log.info("유저 입장: userId={}, chatRoomId={}", userId, chatRoomId);
    }
  }


  @EventListener
  public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

    String userId = (String) headerAccessor.getSessionAttributes().get("userId");
    String chatRoomId = (String) headerAccessor.getSessionAttributes().get("chatRoomId");

    if (userId != null && chatRoomId != null) {
      chatMemberTracker.removeUserFromRoom(chatRoomId, userId);
      log.info("유저 퇴장: userId={}, chatRoomId={}", userId, chatRoomId);
    }
  }
}
