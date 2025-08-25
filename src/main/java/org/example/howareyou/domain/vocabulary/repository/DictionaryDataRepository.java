package org.example.howareyou.domain.vocabulary.repository;

import org.example.howareyou.domain.vocabulary.document.DictionaryData;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DictionaryDataRepository extends MongoRepository<DictionaryData, String>, DictionaryDataRepositoryCustom {
    List<DictionaryData> findByWordIn(List<String> candidateWords);
    // 오답풀 생성하기위한 level 필터링해서 전체반환
    List<DictionaryData> findByLevel(List<String> level, String dictionaryType);


}
