package org.example.howareyou.domain.vocabulary.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.voca.dto.ChatMessageReadModel;
import org.example.howareyou.domain.chat.voca.service.ChatMessageVocaService;
//import org.example.howareyou.domain.vocabulary.document.ChatRoomVocabulary;
import org.example.howareyou.domain.vocabulary.document.ChatRoomVocabulary;
import org.example.howareyou.domain.vocabulary.document.DictionaryData;
import org.example.howareyou.domain.vocabulary.dto.AnalyzeBatchRequest;
import org.example.howareyou.domain.vocabulary.dto.AnalyzedResponseWord;
//import org.example.howareyou.domain.vocabulary.entity.DictionaryData;
//import org.example.howareyou.domain.vocabulary.repository.ChatRoomVocabularyRepository;
import org.example.howareyou.domain.vocabulary.dto.MessageItem;
import org.example.howareyou.domain.vocabulary.dto.WordPosPair;
import org.example.howareyou.domain.vocabulary.repository.ChatRoomVocabularyRepository;
import org.example.howareyou.domain.vocabulary.repository.DictionaryDataRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChatVocaBookService {

    private final ChatMessageVocaService chatMessageVocaService;
    private final NlpClient nlpClient;
    private final DictionaryDataRepository dictionaryDataRepository;
    private final ChatRoomVocabularyRepository chatRoomVocabularyRepository;

    /**
     * ✅ 리액티브 배치 진입점
     * 주어진 시간 범위 [start, end)의 채팅 메시지를 채팅방별로 그룹핑하고,
     * 각 채팅방마다:
     *   1) Json으로 통째로 보내기 → NLP 분석 (비동기)
     *   2) 분석 결과를 사전 데이터와 매칭 (블로킹 → boundedElastic)
     *   3) MongoDB에 단어장 저장 (블로킹 → boundedElastic)
     *
     * 반환: Mono<Void>  (구독 시 작업이 시작되며, 완료 시 onComplete 신호)
     */
    public Mono<Void> generateVocabularyForRangeReactive(Instant start, Instant end) {
        return fetchGroupedMessagesByRoomReactive(start, end)              // 시간 범위 내 메시지 조회 & 채팅방별 그룹핑
                .flatMapMany(map -> Flux.fromIterable(map.entrySet()))    // Map<room, messages> → Flux<Entry> 변환
                .flatMap(entry ->
                                analyzeMessagesAsBatchReactive(entry.getKey(), entry.getValue()) // NLP 결과
                                        .flatMap(analyzedWords ->
                                                matchWordsWithDictionaryReactive(analyzedWords)          // 사전 매칭
                                                        .flatMap(matched ->
                                                                saveVocabularyReactive(entry.getKey(), matched, analyzedWords) // Mongo 저장
                                                        )
                                        ),
                        6 // 동시에 처리할 채팅방 개수 제한
                )
                .then()     // 모든 채팅방 처리 완료 시 Mono<Void> 반환
                .doOnSubscribe(s -> log.info("🚀 채팅방 단어장 생성 시작: {} ~ {}", start, end))
                .doOnTerminate(() -> log.info("✅ 채팅방 단어장 생성 종료"));
    }

    /* ---------- 내부 리액티브 유틸 ---------- */

    /**
     * ✅ (블로킹) 채팅 메시지를 시간 범위로 조회 후, 채팅방별로 그룹핑
     *    - boundedElastic 스레드풀에서 실행 (DB 조회는 블로킹이므로)
     */
    private Mono<Map<String, List<ChatMessageReadModel>>> fetchGroupedMessagesByRoomReactive(Instant start, Instant end) {
        return Mono.fromCallable(() -> chatMessageVocaService.getMessagesInRange(start, end))
                .subscribeOn(Schedulers.boundedElastic())
                .map(messages -> messages.stream().collect(Collectors.groupingBy(ChatMessageReadModel::getChatRoomUuid)));
    }

//    /** NLP 호출: fulltext로 합쳐서 String으로 보내기 */
//    private Mono<List<AnalyzedResponseWord>> analyzeMessagesAsTextReactive(String chatRoomUuid,
//                                                                           List<ChatMessageReadModel> messages) {
//        String fullText = messages.stream()
//                .map(ChatMessageReadModel::getContent)
//                .collect(Collectors.joining(" "));
//        return nlpClient.analyzeReactive(fullText)
//                .doOnNext(list -> log.info("🧠 NLP 완료 - room={}, analyzed={}", chatRoomUuid, list.size()));
//    }

    /**
     * ✅ (비동기) NLP 서버에 배치 분석 요청
     *    - 채팅방의 모든 메시지를 JSON 배열로 변환하여 전달
     *    - NLP 서버 응답: 각 단어의 text, pos, lang 등
     */
    private Mono<List<AnalyzedResponseWord>> analyzeMessagesAsBatchReactive(
            String chatRoomUuid,
            List<ChatMessageReadModel> messages
    ) {
        AnalyzeBatchRequest req = new AnalyzeBatchRequest(
                chatRoomUuid,
                messages.stream()
                        .map(m -> new MessageItem(
                                m.getId(),
                                m.getContent(),
                                m.getSenderId(),
                                m.getSenderName(),
                                m.getMessageTime().toString()
                        ))
                        .toList()
        );

        try {
            String jsonPayload = new ObjectMapper().writeValueAsString(req);
            log.info("📤 NLP 요청 JSON: {}", jsonPayload);
        } catch (Exception e) {
            log.error("❌ NLP 요청 JSON 직렬화 실패", e);
        }

        return nlpClient.analyzeBatchReactive(req)
                .doOnNext(list -> log.info("🧠 NLP 완료 - room={}, analyzed={}", chatRoomUuid, list.size()));
    }

    /**
     * ✅ (블로킹) 분석 결과 단어를 사전 데이터셋과 매칭
     *    - Word + POS 쌍으로 중복 제거 후 MongoDB 조회
     *    - boundedElastic에서 실행
     */
    private Mono<List<DictionaryData>> matchWordsWithDictionaryReactive(List<AnalyzedResponseWord> analyzedWords) {
        return Mono.fromCallable(() -> {
                    List<WordPosPair> pairs = analyzedWords.stream()
                            .map(w -> new WordPosPair(w.getWord(), w.getPos()))
                            .distinct()
                            .toList();
                    return dictionaryDataRepository.findByWordAndPosPairs(pairs);   // MongoDB 사전 검색
                })
                .doOnError(e -> log.error("❌ 사전 매칭 중 에러", e))
                .subscribeOn(Schedulers.boundedElastic())   //블로킹 DB 조회
                .doOnNext(list -> log.info("🔍 사전 매칭 완료 - {}개", list.size()));
    }

    /**
     * ✅ (블로킹) MongoDB에 채팅방 단어장 저장
     *    - 사전 매칭된 단어 리스트를 Document 구조로 변환 후 저장
     *    - boundedElastic에서 실행
     */
    private Mono<Void> saveVocabularyReactive(
            String chatRoomUuid,
            List<DictionaryData> matchedWords,
            List<AnalyzedResponseWord> analyzedWords) {
        return Mono.fromRunnable(() -> {
                    List<ChatRoomVocabulary.DictionaryWordEntry> wordEntries = matchedWords.stream()
                            .map(word -> {
                                // ✅ 이 단어와 품사가 같은 NLP 분석 결과 필터링
                                List<AnalyzedResponseWord> matches = analyzedWords.stream()
                                        .filter(a -> a.getWord().equalsIgnoreCase(word.getWord())
                                                && a.getPos().equalsIgnoreCase(word.getPos()))
                                        .toList();

                                // ✅ 사용된 문장 & 메시지 ID 추출
                                List<String> examples = matches.stream()
                                        .map(AnalyzedResponseWord::getExample)
                                        .distinct()
                                        .toList();

                                List<String> messageIds = matches.stream()
                                        .map(AnalyzedResponseWord::getSourceMessageId)
                                        .distinct()
                                        .toList();

                                return ChatRoomVocabulary.DictionaryWordEntry.builder()
                                        .word(word.getWord())
                                        .meaning(word.getMeaning())
                                        .pos(word.getPos())
                                        .lang(word.getDictionaryType().startsWith("en") ? "en" : "ko")
                                        .level(word.getLevel())
                                        .dictionaryType(word.getDictionaryType())
                                        .usedInMessages(examples)  // ✅ 사용된 문장 저장
                                        .messageIds(messageIds)    // ✅ 채팅 ID 저장
                                        .build();
                            })
                            .toList();

                    ChatRoomVocabulary document = ChatRoomVocabulary.builder()
                            .chatRoomUuid(chatRoomUuid)
                            .analyzedAt(Instant.now())
                            .words(wordEntries)
                            .build();

                    chatRoomVocabularyRepository.save(document);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then()
                .doOnSuccess(v -> log.info("💾 저장 완료 - room={}", chatRoomUuid));
    }

    /**
     * 채팅방 UUID로 전체 단어장 조회 (최신순)
     */
    public List<ChatRoomVocabulary> getAllVocabularyByChatRoom(String chatRoomUuid) {
        return chatRoomVocabularyRepository
                .findByChatRoomUuidOrderByAnalyzedAtDesc(chatRoomUuid);
    }

    /**
     * 전체 채팅방 전체 단어장 조회 (최신순)
     */
    public List<ChatRoomVocabulary> getAllVocabularies() {
        return chatRoomVocabularyRepository.findAllByOrderByAnalyzedAtDesc();
    }


}
