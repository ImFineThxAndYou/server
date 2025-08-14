package org.example.howareyou.domain.chat.websocket.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.entity.ChatRoom;
import org.example.howareyou.domain.chat.repository.ChatRoomRepository;
import org.example.howareyou.domain.chat.websocket.dto.ChatMessageDocumentResponse;
import org.example.howareyou.domain.chat.websocket.dto.ChatMessageResponse;
import org.example.howareyou.domain.chat.websocket.dto.CreateChatMessageRequest;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageStatus;
import org.example.howareyou.domain.chat.websocket.repository.ChatMessageDocumentRepository;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.example.howareyou.domain.notification.service.NotificationPushService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {
  private final ChatMessageDocumentRepository mongoRepository;
  private final ChatRedisService chatRedisService;
  private final ChatRoomRepository chatRoomRepository;
  private final NotificationPushService notificationPushService;
  private final MemberRepository memberRepository;

  /**
   * 채팅 메시지를 Redis 캐시에 저장하고, MongoDB에 영구 저장하는 메서드. - 채팅방 및 상대방 정보 확인 - Redis: 최근 메시지 추가, 안읽은 메시지 수
   * 증가 - MongoDB: 메시지 비동기 저장
   *
   * @return
   */

  @Transactional
  public ChatMessageResponse saveChatMessage(CreateChatMessageRequest request) {

    final String chatRoomUuid = request.getChatRoomUuid();

    // 채팅방 조회
    ChatRoom chatRoom = chatRoomRepository.findByUuid(chatRoomUuid)
        .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

    // 발신자 조회/검증
    Long senderId = request.getSenderId();
    Member sender = memberRepository.findById(senderId)
        .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

    // 수신자 산출 (1:1 기준)
    Member receiver = chatRoom.getOtherParticipant(senderId);
    Long receiverId = receiver.getId();

    // 표시용 이름 스냅샷 (프로필 닉네임 우선 → membername → email)
    String senderName   = sender.getMembername();
    String receiverName = receiver.getMembername();

    // Mongo 저장용 도큐먼트 구성
    ChatMessageDocument messageForMongo = ChatMessageDocument.builder()
        .chatRoomUuid(chatRoomUuid)
        .senderId(String.valueOf(senderId))
        .senderName(senderName)
        .receiverId(String.valueOf(receiverId))
        .receiverName(receiverName)
        .content(request.getContent())
        .messageTime(Instant.now())
        .chatMessageStatus(ChatMessageStatus.UNREAD)
        .build();

    // MongoDB 저장
    messageForMongo = mongoRepository.save(messageForMongo);

    // Redis 최근 메시지 캐시 (id가 채워진 객체로)
    chatRedisService.addRecentMessage(chatRoomUuid, messageForMongo);
    chatRedisService.trimRecentMessages(chatRoomUuid, 30);

    // 수신자 현재 방 체크
    String receiverCurrentRoom = chatRedisService.getCurrentChatRoom(String.valueOf(receiverId));
    boolean isReceiverInRoom = chatRoomUuid.equals(receiverCurrentRoom);

    if (!isReceiverInRoom) {
      // 안읽은 메시지 증가
      chatRedisService.incrementUnread(chatRoomUuid, String.valueOf(receiverId));

      // 알림 발송
      //  - messageId는 Mongo 저장 후 사용
      notificationPushService.sendChatNotify(
          chatRoom.getId(),
          senderId,
          messageForMongo.getId(),
          messageForMongo.getContent(),
          receiverId
      );
    }

    log.debug("채팅 메시지 저장 완료 - roomUuid={}, roomId={}, senderId={}, receiverOnline={}, redis+mongo 저장",
        chatRoomUuid, chatRoom.getId(), senderId, isReceiverInRoom);
    return ChatMessageResponse.from(messageForMongo);
  }


  /**
   * Redis에서 최근 메시지 30개 조회
   */
  public List<ChatMessageDocumentResponse> getRecentMessagesWithFallback(String chatRoomId, int maxCount) {

    // 1. Redis에서 메시지 조회
    List<Object> rawMessages = chatRedisService.getRecentMessages(chatRoomId, maxCount);
    List<ChatMessageDocument> redisMessages = rawMessages.stream()
        .filter(ChatMessageDocument.class::isInstance)
        .map(o -> (ChatMessageDocument) o)
        .toList();

    int redisCount = redisMessages.size();

    List<ChatMessageDocumentResponse> responses = redisMessages.stream()
        .map(ChatMessageDocumentResponse::from)
        .toList();

    // 2. 메시지가 충분하면 그대로 리턴
    if (redisCount >= maxCount) {
      return responses;
    }

    // 3. 부족하면 MongoDB에서 보충
    int remaining = maxCount - redisMessages.size();

    Instant lastMessageTime = redisMessages.isEmpty()
        ? Instant.now()
        : redisMessages.get(redisMessages.size() - 1).getMessageTime(); // 오래된 메시지가 마지막에 위치

    PageRequest pageable = PageRequest.of(0, remaining, Sort.by(Sort.Direction.DESC, "messageTime"));

    List<ChatMessageDocument> mongoMessages =
        mongoRepository.findTopNByChatRoomUuidAndMessageTimeBeforeOrderByMessageTimeDesc(
            chatRoomId, lastMessageTime, pageable
        );


    // 시간 역순 정렬이므로 다시 정방향 정렬
    Collections.reverse(mongoMessages);

    // 4. 결합 후 반환
    List<ChatMessageDocument> result = new ArrayList<>();
      result.addAll(mongoMessages);
      result.addAll(redisMessages);

      return result.stream()
          .map(ChatMessageDocumentResponse::from)
          .toList();
}

  /**
   * 유저가 채팅방에 입장했을 때 호출되는 로직
   * - 안 읽은 메시지 수 초기화
   * - MongoDB 메시지 상태를 읽음(READ)으로 변경
   */
  public void markMessagesAsRead(String chatRoomId, String userId) {

    // 1. Redis에서 안 읽은 메시지 수 초기화
    chatRedisService.resetUnread(chatRoomId, userId);

    // 2. MongoDB에서 해당 채팅방의 해당 유저가 안 읽은 메시지를 모두 읽음 처리
    List<ChatMessageDocument> unreadMessages = mongoRepository
        .findByChatRoomUuidAndSenderNameNotAndChatMessageStatus(chatRoomId, userId, ChatMessageStatus.UNREAD);

    unreadMessages.forEach(msg -> msg.setChatMessageStatus(ChatMessageStatus.READ));

    mongoRepository.saveAll(unreadMessages);

    log.debug("메시지 읽음 처리 완료 - chatRoom={}, userId={}, 총 {}건", chatRoomId, userId,
        unreadMessages.size());
  }

  /**
   * 이전 메시지 페이징
  */
  public List<ChatMessageDocumentResponse> getPreviousMessages(String chatRoomId, Instant before, int size) {
    PageRequest pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "messageTime"));

    List<ChatMessageDocument> mongoMessages =
        mongoRepository.findTopNByChatRoomUuidAndMessageTimeBeforeOrderByMessageTimeDesc(
            chatRoomId, before, pageable
        );

    // 시간 역순으로 가져왔으므로, 정방향으로 정렬
    Collections.reverse(mongoMessages);

    return mongoMessages.stream()
        .map(ChatMessageDocumentResponse::from)
        .toList();
  }

}
