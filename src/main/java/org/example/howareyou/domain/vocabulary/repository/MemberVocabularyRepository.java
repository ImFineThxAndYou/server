package org.example.howareyou.domain.vocabulary.repository;

import org.example.howareyou.domain.vocabulary.document.MemberVocabulary;
import org.example.howareyou.domain.vocabulary.dto.AggregatedWordEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MemberVocabularyRepository extends MongoRepository<MemberVocabulary, String> {
    Page<MemberVocabulary> findByMembername(String membername, Pageable pageable);

    //난이도별 집계
    // ✅ 최신 + 중복 제거 + 난이도 필터 + 페이징
    @Aggregation(pipeline = {
            "{ $match: { membername: ?0 } }",
            "{ $unwind: \"$words\" }",
            // lang/pos는 선택, level은 필수(경로 변수)
            "{ $match: { $expr: { $and: [" +
                    "{ $or: [ { $eq: [ ?1, null ] }, { $eq: [ \"$words.lang\", ?1 ] } ] }," +
                    "{ $or: [ { $eq: [ ?2, null ] }, { $eq: [ \"$words.pos\",  ?2 ] } ] }," +
                    "{ $eq: [ \"$words.level\", ?3 ] }" +
                    "] } } }",
            // 최신 선택을 위해 정렬 후 그룹 + last
            "{ $sort: { \"words.analyzedAt\": 1 } }",
            "{ $group: { " +
                    "_id: { word: \"$words.word\", pos: \"$words.pos\" }," +
                    "analyzedAt: { $last: \"$words.analyzedAt\" }," +
                    "meaning:    { $last: \"$words.meaning\" }," +
                    "lang:       { $last: \"$words.lang\" }," +
                    "level:      { $last: \"$words.level\" }," +
                    "example:    { $last: \"$words.example\" }" +
                    "} }",
            "{ $project: { _id: 0, word: \"$_id.word\", pos: \"$_id.pos\", lang: 1, level: 1, meaning: 1, example: 1, analyzedAt: 1 } }",
            "{ $sort: { analyzedAt: -1 } }",
            "{ $skip: ?4 }",
            "{ $limit: ?5 }"
    })
    List<AggregatedWordEntry> findLatestUniqueWordsByLevel(
            String membername,
            String lang,
            String pos,
            String level,
            int skip,
            int limit
    );

    //totalCount
    @Aggregation(pipeline = {
            "{ $match: { membername: ?0 } }",
            "{ $unwind: \"$words\" }",
            "{ $match: { $expr: { $and: [" +
                    "{ $or: [ { $eq: [ ?1, null ] }, { $eq: [ \"$words.lang\", ?1 ] } ] }," +
                    "{ $or: [ { $eq: [ ?2, null ] }, { $eq: [ \"$words.pos\",  ?2 ] } ] }," +
                    "{ $eq: [ \"$words.level\", ?3 ] }" +
                    "] } } }",
            "{ $sort: { \"words.analyzedAt\": 1 } }",
            "{ $group: { _id: { word: \"$words.word\", pos: \"$words.pos\" } } }",
            "{ $count: \"total\" }"
    })
    List<CountOnly> countLatestUniqueWordsByLevel(
            String membername,
            String lang,
            String pos,
            String level
    );


    // (신규) 전체 조회용: (word,pos) 기준 최신 1개만
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
    List<AggregatedWordEntry> findLatestUniqueWords(String membername,
                                                    String lang,
                                                    String pos,
                                                    int skip,
                                                    int limit);

    // totalCount
    @Aggregation(pipeline = {
            "{ $match: { membername: ?0 } }",
            "{ $unwind: \"$words\" }",
            "{ $match: { $expr: { $and: [" +
                    "{ $or: [ { $eq: [ ?1, null ] }, { $eq: [ \"$words.lang\", ?1 ] } ] }," +
                    "{ $or: [ { $eq: [ ?2, null ] }, { $eq: [ \"$words.pos\",  ?2 ] } ] }" +
                    "] } } }",
            "{ $group: { _id: { word: \"$words.word\", pos: \"$words.pos\" } } }",
            "{ $count: \"total\" }"
    })
    List<CountOnly> countLatestUniqueWords(String membername, String lang, String pos);

    interface CountOnly { Long getTotal(); }

    /* -------------------- 대시보드용 메서드들 -------------------- */

    /**
     * 전체 단어 개수 조회 (기간별 필터링 없음)
     */
    @Aggregation(pipeline = {
            "{ $match: { membername: ?0 } }",
            "{ $unwind: \"$words\" }",
            "{ $group: { _id: { word: \"$words.word\", pos: \"$words.pos\" } } }",
            "{ $count: \"total\" }"
    })
    List<CountOnly> countTotalUniqueWords(String membername);

    /**
     * 기간별 총 단어 개수 조회
     */
    @Aggregation(pipeline = {
            "{ $match: { membername: ?0 } }",
            "{ $unwind: \"$words\" }",
            "{ $addFields: { " +
                    "localDate: { " +
                    "  $dateToString: { " +
                    "    format: \"%Y-%m-%d\", " +
                    "    date: { $dateFromString: { dateString: \"$words.analyzedAt\" } }, " +
                    "    timezone: ?1 " + // timezone parameter
                    "  } " +
                    "} } }",
            "{ $match: { " +
                    "localDate: { " +
                    "  $gte: ?2, " + // fromDate parameter
                    "  $lte: ?3 " + // toDate parameter
                    "} } }",
            "{ $group: { _id: { word: \"$words.word\", pos: \"$words.pos\" } } }",
            "{ $count: \"total\" }"
    })
    List<CountOnly> countTotalUniqueWordsByPeriod(String membername, String timezone, String fromDate, String toDate);

    /**
     * 언어별 기간 필터링 단어 개수 조회
     */
    @Aggregation(pipeline = {
            "{ $match: { membername: ?0 } }",
            "{ $unwind: \"$words\" }",
            "{ $addFields: { " +
                    "localDate: { " +
                    "  $dateToString: { " +
                    "    format: \"%Y-%m-%d\", " +
                    "    date: { $dateFromString: { dateString: \"$words.analyzedAt\" } }, " +
                    "    timezone: ?1 " + // timezone parameter
                    "  } " +
                    "} } }",
            "{ $match: { " +
                    "localDate: { " +
                    "  $gte: ?2, " + // fromDate parameter
                    "  $lte: ?3 " + // toDate parameter
                    "}, " +
                    "words.lang: ?4 " + // lang parameter
                    "} } }",
            "{ $group: { _id: { word: \"$words.word\", pos: \"$words.pos\" } } }",
            "{ $count: \"total\" }"
    })
    List<CountOnly> countByMemberAndLangAndPeriod(String membername, String timezone, String fromDate, String toDate, String lang);

    /**
     * 품사별 기간 필터링 단어 개수 조회
     */
    @Aggregation(pipeline = {
            "{ $match: { membername: ?0 } }",
            "{ $unwind: \"$words\" }",
            "{ $addFields: { " +
                    "localDate: { " +
                    "  $dateToString: { " +
                    "    format: \"%Y-%m-%d\", " +
                    "    date: { $dateFromString: { dateString: \"$words.analyzedAt\" } }, " +
                    "    timezone: ?1 " + // timezone parameter
                    "  } " +
                    "} } }",
            "{ $match: { " +
                    "localDate: { " +
                    "  $gte: ?2, " + // fromDate parameter
                    "  $lte: ?3 " + // toDate parameter
                    "}, " +
                    "words.pos: ?4 " + // pos parameter
                    "} } }",
            "{ $group: { _id: { word: \"$words.word\", pos: \"$words.pos\" } } }",
            "{ $count: \"total\" }"
    })
    List<CountOnly> countByMemberAndPosAndPeriod(String membername, String timezone, String fromDate, String toDate, String pos);

    /**
     * 언어+품사별 기간 필터링 단어 개수 조회
     */
    @Aggregation(pipeline = {
            "{ $match: { membername: ?0 } }",
            "{ $unwind: \"$words\" }",
            "{ $addFields: { " +
                    "localDate: { " +
                    "  $dateToString: { " +
                    "    format: \"%Y-%m-%d\", " +
                    "    date: { $dateFromString: { dateString: \"$words.analyzedAt\" } }, " +
                    "    timezone: ?1 " + // timezone parameter
                    "  } " +
                    "} } }",
            "{ $match: { " +
                    "localDate: { " +
                    "  $gte: ?2, " + // fromDate parameter
                    "  $lte: ?3 " + // toDate parameter
                    "}, " +
                    "words.lang: ?4, " + // lang parameter
                    "words.pos: ?5 " + // pos parameter
                    "} } }",
            "{ $group: { _id: { word: \"$words.word\", pos: \"$words.pos\" } } }",
            "{ $count: \"total\" }"
    })
    List<CountOnly> countByMemberAndLangAndPosAndPeriod(String membername, String timezone, String fromDate, String toDate, String lang, String pos);

    /**
     * 기간별 일일 단어 개수 조회
     */
    @Aggregation(pipeline = {
            "{ $match: { membername: ?0 } }",
            "{ $unwind: \"$words\" }",
            "{ $addFields: { " +
                    "localDate: { " +
                    "  $dateToString: { " +
                    "    format: \"%Y-%m-%d\", " +
                    "    date: { $dateFromString: { dateString: \"$words.analyzedAt\" } }, " +
                    "    timezone: ?1 " + // timezone parameter
                    "  } " +
                    "} } }",
            "{ $match: { " +
                    "localDate: { " +
                    "  $gte: ?2, " + // fromDate parameter
                    "  $lte: ?3 " + // toDate parameter
                    "} } }",
            "{ $group: { " +
                    "_id: \"$localDate\", " +
                    "count: { $sum: 1 } " +
                    "} }",
            "{ $sort: { _id: 1 } }"
    })
    List<DailyWordCount> getDailyWordCountsByPeriod(String membername, String timezone, String fromDate, String toDate);

    /**
     * 일일 단어 개수 결과를 위한 인터페이스
     */
    interface DailyWordCount {
        String get_id(); // MongoDB aggregation의 _id 필드
        Integer getCount();
    }
}