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
  private final ChatRedisService chatRedisService;

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

    // 2. Redis에 최근 메시지 캐싱
    chatRedisService.addRecentMessage(chatMessage.getChatRoomUuid(), saved);

    log.debug("채팅 메시지 저장 완료 - Mongo + Redis");
  }
}
