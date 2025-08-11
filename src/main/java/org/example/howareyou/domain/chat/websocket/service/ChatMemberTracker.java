package org.example.howareyou.domain.chat.websocket.service;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ChatMemberTracker {

  private final RedisTemplate<String, String> redisTemplate;

  private static final String CHAT_ROOM_USER_KEY_PREFIX = "chatroom:";

  public void addUserToRoom(String chatRoomId, String userId) {
    String key = getRoomKey(chatRoomId);
    redisTemplate.opsForSet().add(key, userId);
    redisTemplate.expire(key, Duration.ofMinutes(10)); // TTL 10분
  }

  public void removeUserFromRoom(String chatRoomId, String userId) {
    String key = getRoomKey(chatRoomId);
    redisTemplate.opsForSet().remove(key, userId);
  }

  private String getRoomKey(String chatRoomId) {
    return CHAT_ROOM_USER_KEY_PREFIX + chatRoomId + ":users";
  }

}
