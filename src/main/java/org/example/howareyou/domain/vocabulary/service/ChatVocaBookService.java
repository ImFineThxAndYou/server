package org.example.howareyou.domain.vocabulary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.voca.dto.ChatMessageReadModel;
import org.example.howareyou.domain.chat.voca.service.ChatMessageVocaService;
//import org.example.howareyou.domain.vocabulary.document.ChatRoomVocabulary;
import org.example.howareyou.domain.vocabulary.document.ChatRoomVocabulary;
import org.example.howareyou.domain.vocabulary.document.DictionaryData;
import org.example.howareyou.domain.vocabulary.dto.AnalyzedResponseWord;
//import org.example.howareyou.domain.vocabulary.entity.DictionaryData;
//import org.example.howareyou.domain.vocabulary.repository.ChatRoomVocabularyRepository;
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
     *   1) 메시지 전체 텍스트 생성 → NLP 분석 (비동기)
     *   2) 분석 결과를 사전 데이터와 매칭 (블로킹 → boundedElastic)
     *   3) MongoDB에 단어장 저장 (블로킹 → boundedElastic)
     *
     * 반환: Mono<Void>  (구독 시 작업이 시작되며, 완료 시 onComplete 신호)
     */
    public Mono<Void> generateVocabularyForRangeReactive(Instant start, Instant end) {
        return fetchGroupedMessagesByRoomReactive(start, end)              // Mono<Map<room, messages>>
                .flatMapMany(map -> Flux.fromIterable(map.entrySet()))    // Flux<Map.Entry<...>>
                .flatMap(entry ->
                                analyzeMessagesAsTextReactive(entry.getKey(), entry.getValue())    // Mono<List<Analyzed...>>
                                        .flatMap(this::matchWordsWithDictionaryReactive)          // Mono<List<DictionaryData>>
                                        .flatMap(matched -> saveVocabularyReactive(entry.getKey(), matched)),
                        /* 동시성 제한 */ 6
                )
                .then()
                .doOnSubscribe(s -> log.info("🚀 채팅방 단어장 생성 시작: {} ~ {}", start, end))
                .doOnTerminate(() -> log.info("✅ 채팅방 단어장 생성 종료"));
    }

    /* ---------- 내부 리액티브 유틸 ---------- */

    /** 메시지 조회(블로킹 가정) → boundedElastic에서 실행 */
    private Mono<Map<String, List<ChatMessageReadModel>>> fetchGroupedMessagesByRoomReactive(Instant start, Instant end) {
        return Mono.fromCallable(() -> chatMessageVocaService.getMessagesInRange(start, end))
                .subscribeOn(Schedulers.boundedElastic())
                .map(messages -> messages.stream().collect(Collectors.groupingBy(ChatMessageReadModel::getChatRoomUuid)));
    }

    /** NLP 호출: ✅ 논블로킹 */
    private Mono<List<AnalyzedResponseWord>> analyzeMessagesAsTextReactive(String chatRoomUuid,
                                                                           List<ChatMessageReadModel> messages) {
        String fullText = messages.stream()
                .map(ChatMessageReadModel::getContent)
                .collect(Collectors.joining(" "));
        return nlpClient.analyzeReactive(fullText)
                .doOnNext(list -> log.info("🧠 NLP 완료 - room={}, analyzed={}", chatRoomUuid, list.size()));
    }

    /** 사전 매칭: 블로킹 → boundedElastic */
    private Mono<List<DictionaryData>> matchWordsWithDictionaryReactive(List<AnalyzedResponseWord> analyzedWords) {
        return Mono.fromCallable(() -> {
                    List<WordPosPair> pairs = analyzedWords.stream()
                            .map(w -> new WordPosPair(w.getText(), w.getPos()))
                            .distinct()
                            .toList();
                    return dictionaryDataRepository.findByWordAndPosPairs(pairs);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("🔍 사전 매칭 완료 - {}개", list.size()));
    }

    /** Mongo 저장: 블로킹 → boundedElastic */
    private Mono<Void> saveVocabularyReactive(String chatRoomUuid, List<DictionaryData> matchedWords) {
        return Mono.fromRunnable(() -> {
                    List<ChatRoomVocabulary.DictionaryWordEntry> wordEntries = matchedWords.stream()
                            .map(word -> ChatRoomVocabulary.DictionaryWordEntry.builder()
                                    .word(word.getWord())
                                    .meaning(word.getMeaning())
                                    .pos(word.getPos())
                                    .lang(word.getDictionaryType().startsWith("en") ? "en" : "ko")
                                    .level(word.getLevel())
                                    .dictionaryType(word.getDictionaryType())
                                    .build())
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
