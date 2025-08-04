package org.example.howareyou.domain.vocabulary.repository;

import org.example.howareyou.domain.vocabulary.document.DictionaryData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DictionaryDataRepository extends MongoRepository<DictionaryData, String> {
}
