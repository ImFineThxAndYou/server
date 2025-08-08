package org.example.howareyou.domain.vocabulary.repository;


import org.example.howareyou.domain.vocabulary.document.ChatRoomVocabulary;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatRoomVocabularyRepository extends MongoRepository<ChatRoomVocabulary, String> {
}
