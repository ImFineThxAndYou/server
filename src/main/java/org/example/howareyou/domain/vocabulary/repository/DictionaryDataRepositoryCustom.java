package org.example.howareyou.domain.vocabulary.repository;

import org.example.howareyou.domain.vocabulary.document.DictionaryData;
import org.example.howareyou.domain.vocabulary.dto.WordPosPair;

import java.util.List;

public interface DictionaryDataRepositoryCustom {
    List<DictionaryData> findByWordAndPosPairs(List<WordPosPair> pairs);

}
