package org.example.howareyou.domain.chat.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.kafka.dto.ChatMessageCreatedEvent;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageStatus;
import org.example.howareyou.domain.chat.websocket.repository.ChatMessageDocumentRepository;
import org.example.howareyou.domain.chat.websocket.service.ChatRedisService;
import org.example.howareyou.domain.notification.service.NotificationPushService;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessageEventConsumer {

  private final ChatRedisService chatRedisService;
  private final NotificationPushService notificationPushService;
  private final ObjectMapper objectMapper;
  private final ChatMessageDocumentRepository mongoRepository;

  @KafkaListener(topics = "chat.message.created", groupId = "chat-consumer")
  public void handleChatMessageCreated(String payload) throws JsonProcessingException {
    ChatMessageCreatedEvent event = objectMapper.readValue(payload, ChatMessageCreatedEvent.class);

    // --- Mongo 저장
    ChatMessageDocument doc = mongoRepository.save(
        ChatMessageDocument.builder()
            .chatRoomUuid(event.getRoomUuid())
            .senderId(String.valueOf(event.getSenderId()))
            .receiverId(String.valueOf(event.getReceiverId()))
            .content(event.getContent())
            .messageTime(Instant.now())
            .chatMessageStatus(ChatMessageStatus.UNREAD)
            .build()
    );

    // --- Redis 캐시
    chatRedisService.addRecentMessage(event.getRoomUuid(), doc);
    chatRedisService.trimRecentMessages(event.getRoomUuid(), 30);

    // --- 알림 발송
    notificationPushService.sendChatNotify(
        event.getRoomId(),
        event.getSenderId(),
        doc.getId(),
        doc.getContent(),
        event.getReceiverId()
    );

    log.info("[ASYNC] Message saved + cached + notified. room={}, msgId={}",
        event.getRoomUuid(), doc.getId());
  }


}
