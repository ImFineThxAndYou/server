package org.example.howareyou.domain.chat.websocket.repository;

import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface ChatMessageDocumentRepository extends
    MongoRepository<ChatMessageDocument, String> {
    @Query(value = "{ 'chatRoomUuid' : ?0 }", sort = "{ 'messageTime' : -1 }")
    Optional<ChatMessageDocument> findLatestMessageByChatRoom(String chatRoomUuid);
}
