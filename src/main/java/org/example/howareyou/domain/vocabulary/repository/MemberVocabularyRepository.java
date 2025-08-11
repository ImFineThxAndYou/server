package org.example.howareyou.domain.vocabulary.repository;

import org.example.howareyou.domain.vocabulary.document.MemberVocabulary;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MemberVocabularyRepository extends MongoRepository<MemberVocabulary, String> {
    List<MemberVocabulary> findByMembername(String membername);
}