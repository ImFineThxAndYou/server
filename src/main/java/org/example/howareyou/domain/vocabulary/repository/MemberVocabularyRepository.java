package org.example.howareyou.domain.vocabulary.repository;

import org.example.howareyou.domain.vocabulary.document.MemberVocabulary;
import org.example.howareyou.domain.vocabulary.dto.AggregatedWordEntry;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MemberVocabularyRepository extends MongoRepository<MemberVocabulary, String> {
    List<MemberVocabulary> findByMembername(String membername);

    @Aggregation(pipeline = {
            // 1) 사용자 전체 문서
            "{ $match: { membername: ?0 } }",
            // 2) words 풀기
            "{ $unwind: \"$words\" }",
            // 3) level/lang 필터 (파라미터가 null이면 패스)
            "{ $match: { $expr: { $and: [" +
                    "{ $or: [ { $eq: [ ?1, null ] }, { $eq: [ \"$words.level\", ?1 ] } ] }," +
                    "{ $or: [ { $eq: [ ?2, null ] }, { $eq: [ \"$words.lang\",  ?2 ] } ] }" +
                    "] } } }",
            // 4) 최신 분석 시각 기준 필드 선택을 위해 정렬
            "{ $sort: { \"words.analyzedAt\": 1 } }",
            // 5) 그룹: (word,pos,lang,level)
            "{ $group: { " +
                    "_id: { word: \"$words.word\", pos: \"$words.pos\", lang: \"$words.lang\", level: \"$words.level\" }," +
                    "frequency:  { $sum: { $ifNull: [ \"$words.frequency\", 1 ] } }," +
                    "analyzedAt: { $last: \"$words.analyzedAt\" }," +
                    "meaning:    { $last: \"$words.meaning\" }," +
                    "chatRoomUuid: { $last: \"$words.chatRoomUuid\" }," +
                    "chatMessageId: { $last: \"$words.chatMessageId\" }," +
                    "example:   { $last: \"$words.example\" }" +
                    "} }",
            // 6) 평탄화
            "{ $project: { _id: 0, " +
                    "word: \"$_id.word\", pos: \"$_id.pos\", lang: \"$_id.lang\", level: \"$_id.level\"," +
                    "frequency: 1, analyzedAt: 1, meaning: 1, chatRoomUuid: 1, chatMessageId: 1, example: 1 } }",
            // 7) 빈도 정렬 (서비스에서 limit 적용 권장)
            "{ $sort: { frequency: -1 } }"
    })
    List<AggregatedWordEntry> aggregateByLevel(String membername, String level, String lang);

}