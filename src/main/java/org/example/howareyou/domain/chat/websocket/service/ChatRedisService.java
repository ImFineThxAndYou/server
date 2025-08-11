package org.example.howareyou.domain.chat.websocket.service;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.chat.websocket.dto.ChatNotificationDTO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRedisService {

  private final RedisTemplate<String, Object> redisTemplate;

  // Redis에 저장될 키 prefix들
  private static final String RECENT_MSG_KEY = "chat:recent:";     // 채팅방별 최근 메시지 리스트
  private static final String UNREAD_MSG_KEY = "chat:unread:";     // 채팅방-유저별 읽지 않은 메시지 수
  private static final String CURRENT_ROOM_KEY = "chat:connect:";  // 유저별 현재 접속중인 채팅방 ID

  // 알림을 redis에서 사용하는 경우
  //  private static final String NOTIFY_KEY = "chat:notify:";         // 유저별 알림용 메시지 DTO

  private static final Duration DEFAULT_TTL = Duration.ofMinutes(10); // Redis에 저장될 데이터 TTL (10분)

  // 채팅방별 최근 메시지 저장 (List 구조 사용)
  public void addRecentMessage(String chatRoomId, Object messageDto) {
    String key = RECENT_MSG_KEY + chatRoomId;
    redisTemplate.opsForList().rightPush(key, messageDto); // 리스트 오른쪽에 메시지 추가
    redisTemplate.expire(key, DEFAULT_TTL); // TTL 설정
  }

  // 채팅방의 최근 메시지 N개 조회
  public List<Object> getRecentMessages(String chatRoomId, long count) {
    String key = RECENT_MSG_KEY + chatRoomId;
    return redisTemplate.opsForList().range(key, -count, -1); // 가장 최근 N개
  }

  // 채팅방 메시지 리스트를 최대 maxCount 개수로 제한 (리스트 사이즈 관리)
  public void trimRecentMessages(String chatRoomId, int maxCount) {
    String key = RECENT_MSG_KEY + chatRoomId;
    redisTemplate.opsForList().trim(key, -maxCount, -1); // 최근 maxCount개만 유지
  }

  // 읽지 않은 메시지 수 증가 (1씩 증가)
  public void incrementUnread(String chatRoomId, String userId) {
    String key = UNREAD_MSG_KEY + chatRoomId + ":" + userId;
    redisTemplate.opsForValue().increment(key); // 숫자 1 증가
//    redisTemplate.expire(key, DEFAULT_TTL); // TTL 설정 안함
  }

  // 읽지 않은 메시지 수 초기화 (메시지 읽음 처리)
  public void resetUnread(String chatRoomId, String userId) {
    String key = UNREAD_MSG_KEY + chatRoomId + ":" + userId;
    redisTemplate.delete(key); // 키 삭제
  }

  // 현재 유저의 읽지 않은 메시지 수 조회
  public int getUnreadCount(String chatRoomId, String userId) {
    String key = UNREAD_MSG_KEY + chatRoomId + ":" + userId;
    Object val = redisTemplate.opsForValue().get(key);
    return val != null ? Integer.parseInt(val.toString()) : 0; // null이면 0 반환
  }

//  // 알림 메시지 저장 (알림용 DTO를 Redis에 저장)
//  public void saveNotification(String userId, String chatRoomId, ChatNotificationDTO notificationDto) {
//    String key = NOTIFY_KEY + userId + ":" + chatRoomId;
//    redisTemplate.opsForValue().set(key, notificationDto, DEFAULT_TTL); // 저장 및 TTL 설정
//  }
//
//  // 저장된 알림 메시지 조회
//  public Object getNotification(String userId, String chatRoomId) {
//    String key = NOTIFY_KEY + userId + ":" + chatRoomId;
//    return redisTemplate.opsForValue().get(key); // 알림 메시지 반환
//  }

  // 유저가 현재 접속중인 채팅방 ID 저장
  public void setCurrentChatRoom(String userId, String chatRoomId) {
    redisTemplate.opsForValue().set(CURRENT_ROOM_KEY + userId, chatRoomId, DEFAULT_TTL); // 채팅방 ID 저장
  }

  // 유저의 현재 접속중인 채팅방 ID 조회
  public String getCurrentChatRoom(String userId) {
    Object val = redisTemplate.opsForValue().get(CURRENT_ROOM_KEY + userId);
    return val != null ? val.toString() : null; // 없으면 null 반환
  }

  // 유저가 채팅방에서 나가거나 연결 종료시 접속 정보 삭제
  public void clearCurrentChatRoom(String userId) {
    redisTemplate.delete(CURRENT_ROOM_KEY + userId);
  }
}
