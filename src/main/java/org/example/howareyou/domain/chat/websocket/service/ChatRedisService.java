package org.example.howareyou.domain.chat.websocket.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.chat.websocket.dto.ChatNotificationDTO;
import org.example.howareyou.domain.chat.websocket.dto.ChatPreviewDTO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRedisService {

  private final RedisTemplate<String, Object> redisTemplate;

  private static final String RECENT_MSG_KEY = "chat:recent:"; // List
  private static final String UNREAD_MSG_KEY = "unread:chat:"; // Value
  private static final String NOTIFY_KEY = "chat:notify:"; // Value
  private static final String CURRENT_ROOM_KEY = "user:currentChatRoom:"; // Value
  private static final String CHAT_LIST_KEY = "chat:list:"; // Hash

  private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

  // 최근 메시지 추가
  public void addRecentMessage(String chatRoomId, Object messageDto) {
    String key = RECENT_MSG_KEY + chatRoomId;
    redisTemplate.opsForList().rightPush(key, messageDto);
    redisTemplate.expire(key, DEFAULT_TTL);
  }

  public List<Object> getRecentMessages(String chatRoomId, long count) {
    String key = RECENT_MSG_KEY + chatRoomId;
    return redisTemplate.opsForList().range(key, -count, -1);
  }

  // 안 읽은 메시지 수 증가 / 초기화
  public void incrementUnread(String chatRoomId, String userId) {
    String key = UNREAD_MSG_KEY + chatRoomId + ":" + userId;
    redisTemplate.opsForValue().increment(key);
    redisTemplate.expire(key, DEFAULT_TTL);
  }

  public void resetUnread(String chatRoomId, String userId) {
    String key = UNREAD_MSG_KEY + chatRoomId + ":" + userId;
    redisTemplate.delete(key);
  }

  public int getUnreadCount(String chatRoomId, String userId) {
    String key = UNREAD_MSG_KEY + chatRoomId + ":" + userId;
    Object val = redisTemplate.opsForValue().get(key);
    return val != null ? Integer.parseInt(val.toString()) : 0;
  }

  // 알림용 DTO 저장
  public void saveNotification(String userId, String chatRoomId, ChatNotificationDTO notificationDto) {
    String key = NOTIFY_KEY + userId + ":" + chatRoomId;
    redisTemplate.opsForValue().set(key, notificationDto, DEFAULT_TTL);
  }

  public Object getNotification(String userId, String chatRoomId) {
    String key = NOTIFY_KEY + userId + ":" + chatRoomId;
    return redisTemplate.opsForValue().get(key);
  }

  // 현재 접속중인 채팅방 ID
  public void setCurrentChatRoom(String userId, String chatRoomId) {
    redisTemplate.opsForValue().set(CURRENT_ROOM_KEY + userId, chatRoomId, DEFAULT_TTL);
  }

  public String getCurrentChatRoom(String userId) {
    Object val = redisTemplate.opsForValue().get(CURRENT_ROOM_KEY + userId);
    return val != null ? val.toString() : null;
  }

  public void clearCurrentChatRoom(String userId) {
    redisTemplate.delete(CURRENT_ROOM_KEY + userId);
  }

}
