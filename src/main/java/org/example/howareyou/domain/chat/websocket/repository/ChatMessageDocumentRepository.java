package org.example.howareyou.domain.chat.websocket.repository;

import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageDocumentRepository extends
    MongoRepository<ChatMessageDocument, String> {

}
