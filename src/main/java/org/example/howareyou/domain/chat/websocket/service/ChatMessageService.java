package org.example.howareyou.domain.chat.websocket.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageStatus;
import org.example.howareyou.domain.chat.websocket.repository.ChatMessageDocumentRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {
  private final ChatMessageDocumentRepository mongoRepository;
  private final RedisTemplate<String, ChatMessageDocument> redisTemplate;

  public void saveChatMessage(ChatMessageDocument chatMessage) {
    // 1. MongoDB 저장
    ChatMessageDocument saved = mongoRepository.save(
        ChatMessageDocument.builder()
            .chatRoomUuid(chatMessage.getChatRoomUuid())
            .sender(chatMessage.getSender())
            .content(chatMessage.getContent())
            .messageTime(Instant.now())
            .chatMessageStatus(ChatMessageStatus.UNREAD)
            .build()
    );

    // 2. Redis 저장 - 최근 메시지 리스트
    String redisKey = "chat:recent:" + chatMessage.getChatRoomUuid();

    try {
      // push to List (rightPush 기준이면 오래된 게 먼저 나옴)
      redisTemplate.opsForList().rightPush(redisKey, saved);

      // TTL 설정
      redisTemplate.expire(redisKey, Duration.ofMinutes(10));

      log.debug("메시지를 Redis에 캐싱했습니다. key={}, msg={}", redisKey, saved.getContent());
    } catch (Exception e) {
      log.warn("Redis 캐싱 실패 - {}", e.getMessage());
    }
  }

}
