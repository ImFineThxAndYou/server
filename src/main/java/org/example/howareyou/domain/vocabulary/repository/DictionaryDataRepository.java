package org.example.howareyou.domain.vocabulary.repository;

import org.example.howareyou.domain.vocabulary.document.DictionaryData;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DictionaryDataRepository extends MongoRepository<DictionaryData, String>, DictionaryDataRepositoryCustom {
    List<DictionaryData> findByWordIn(List<String> candidateWords);


}
