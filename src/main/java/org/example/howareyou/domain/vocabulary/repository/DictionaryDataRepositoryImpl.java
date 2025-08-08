package org.example.howareyou.domain.vocabulary.repository;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.vocabulary.document.DictionaryData;
import org.example.howareyou.domain.vocabulary.dto.WordPosPair;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DictionaryDataRepositoryImpl implements DictionaryDataRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<DictionaryData> findByWordAndPosPairs(List<WordPosPair> pairs) {
        List<Criteria> criteriaList = pairs.stream()
                .map(pair -> Criteria.where("word").is(pair.word())
                        .and("pos").is(pair.pos()))
                .toList();

        Query query = new Query(new Criteria().orOperator(criteriaList));
        return mongoTemplate.find(query, DictionaryData.class);
    }
}
