package org.example.howareyou.domain.quiz.repository;

import org.example.howareyou.domain.vocabulary.document.MemberVocabulary;
import org.example.howareyou.domain.quiz.dto.VocaDTO;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface QuizVocaRepository extends MongoRepository<MemberVocabulary, String> {
    // 전체 조회용: (word,pos) 기준 최신 1개만
    @Aggregation(pipeline = {
            // 1) 사용자 전체 문서
            "{ $match: { membername: ?0 } }",
            // 2) words 언와인드
            "{ $unwind: \"$words\" }",
            // 3) (선택) lang/pos 필터 (파라미터 null이면 통과)
            "{ $match: { $expr: { $and: [" +
                    "{ $or: [ { $eq: [ ?1, null ] }, { $eq: [ \"$words.lang\", ?1 ] } ] }," +
                    "{ $or: [ { $eq: [ ?2, null ] }, { $eq: [ \"$words.pos\",  ?2 ] } ] }" +
                    "] } } }",
            // 4) 최신 선택 위해 analyzedAt desc 정렬
            "{ $sort: { \"words.analyzedAt\": -1 } }",
            // 5) (word,pos)로 그룹하고 $first로 최신 항목만 선택
            "{ $group: { " +
                    "_id: { word: \"$words.word\", pos: \"$words.pos\" }," +
                    "word:          { $first: \"$words.word\" }," +
                    "meaning:       { $first: \"$words.meaning\" }," +
                    "pos:           { $first: \"$words.pos\" }," +
                    "lang:          { $first: \"$words.lang\" }," +
                    "level:         { $first: \"$words.level\" }," +
                    "dictionaryType:{ $first: \"$words.dictionaryType\" }," +
                    "chatRoomUuid:  { $first: \"$words.chatRoomUuid\" }," +
                    "chatMessageId: { $first: \"$words.chatMessageId\" }," +
                    "example:       { $first: \"$words.example\" }," +
                    "analyzedAt:    { $first: \"$words.analyzedAt\" }" +
                    "} }",
            // 6) 화면 정렬: 최신 우선 (필드명 동적 파라미터는 @Aggregation에서 곤란 → 기본값 analyzedAt desc)
            "{ $sort: { analyzedAt: -1 } }",
            // 7) 페이징
            "{ $skip:  ?3 }",
            "{ $limit: ?4 }",
            // 8) 평탄화(project) — DTO에 맞춰 alias
            "{ $project: { _id: 0, word: 1, meaning: 1, pos: 1, lang: 1, level: 1, dictionaryType: 1," +
                    "chatRoomUuid: 1, chatMessageId: 1, example: 1, analyzedAt: 1} }"
    })
    List<VocaDTO> findWords(String membername,
                            String lang,
                            String pos,
                            int skip,
                            int limit);
}
