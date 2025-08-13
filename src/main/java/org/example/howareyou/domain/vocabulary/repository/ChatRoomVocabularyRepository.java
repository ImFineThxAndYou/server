package org.example.howareyou.domain.vocabulary.repository;


import org.example.howareyou.domain.vocabulary.document.ChatRoomVocabulary;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface ChatRoomVocabularyRepository extends MongoRepository<ChatRoomVocabulary, String> {
    List<ChatRoomVocabulary> findByChatRoomUuidOrderByAnalyzedAtDesc(String chatRoomUuid);

    List<ChatRoomVocabulary> findAllByOrderByAnalyzedAtDesc();

    List<ChatRoomVocabulary> findByAnalyzedAtBetween(Instant start, Instant end);
}
