package org.example.howareyou.domain.chat.websocket.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

  @Override
  public boolean beforeHandshake(ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) throws Exception {

    if (request instanceof ServletServerHttpRequest servletRequest) {
      HttpServletRequest httpServletRequest = servletRequest.getServletRequest();

      String userId = httpServletRequest.getParameter("userId");
      String chatRoomId = httpServletRequest.getParameter("chatRoomId");

      attributes.put("userId", userId);
      attributes.put("chatRoomId", chatRoomId);
    }

    return true;
  }

  @Override
  public void afterHandshake(ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception ex) {
    // 생략 가능
  }
}
