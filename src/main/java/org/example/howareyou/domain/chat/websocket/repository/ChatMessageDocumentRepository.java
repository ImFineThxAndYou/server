package org.example.howareyou.domain.chat.websocket.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface ChatMessageDocumentRepository extends
    MongoRepository<ChatMessageDocument, String> {
    @Query(value = "{ 'chatRoomUuid' : ?0 }", sort = "{ 'messageTime' : -1 }")
    Optional<ChatMessageDocument> findLatestMessageByChatRoom(String chatRoomUuid);
public interface ChatMessageDocumentRepository extends MongoRepository<ChatMessageDocument, String> {

  @Query(value = "{'chatRoomUuid': ?0, 'messageTime': { '$lt': ?1 }}")
  List<ChatMessageDocument> findTopNByChatRoomUuidAndMessageTimeBeforeOrderByMessageTimeDesc(
      String chatRoomUuid,
      Instant messageTimeBefore,
      Pageable pageable
  );

  List<ChatMessageDocument> findByChatRoomUuidAndSenderNotAndChatMessageStatus(
      String chatRoomUuid,
      String senderId,
      ChatMessageStatus status
  );

  Optional<ChatMessageDocument> findTopByChatRoomUuidOrderByMessageTimeDesc(String chatRoomUuid);

}
