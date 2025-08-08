package org.example.howareyou.domain.vocabulary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.voca.dto.ChatMessageReadModel;
import org.example.howareyou.domain.chat.voca.service.ChatMessageVocaService;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.example.howareyou.domain.chat.websocket.service.ChatMessageService;
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

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class VocaBookService {

    private final ChatMessageVocaService chatMessageVocaService;
    private final NlpClient nlpClient;
    private final DictionaryDataRepository dictionaryDataRepository;
    private final ChatRoomVocabularyRepository chatRoomVocabularyRepository;

    /**
     * 1시간 단위로 실행되는 단어장 생성 배치 메서드
     */
    public void generateVocabularyForLastHour(Instant start, Instant end) {
        log.info("🚀 단어장 생성 시작: {} ~ {}", start, end); // [1]

        List<ChatMessageReadModel> messages = chatMessageVocaService.getMessagesInRange(start, end);
        log.info("💬 메시지 총 수: {}", messages.size()); // [2]

        // 1️⃣ 주어진 시간 범위 내 모든 메시지를 채팅방 기준으로 그룹핑
        Map<String, List<ChatMessageReadModel>> groupedMessages = fetchGroupedMessagesByRoom(start, end);
        log.info("📦 채팅방 수 (그룹핑된): {}", groupedMessages.size()); // [3]

        // 2️⃣ 각 채팅방별로 NLP 분석 및 단어장 생성 처리
        for (Map.Entry<String, List<ChatMessageReadModel>> entry : groupedMessages.entrySet()) {
            String chatRoomUuid = entry.getKey();
            List<ChatMessageReadModel> roomMessages = entry.getValue();
            log.info("▶️ 분석 시작 - 채팅방 UUID: {}, 메시지 수: {}", chatRoomUuid, roomMessages.size()); // [4]

            // 3️⃣ NLP 분석 수행
            List<AnalyzedResponseWord> analyzedWords = analyzeMessagesAsText(chatRoomUuid, roomMessages);

            // 4️⃣ 사전 데이터셋과 매칭하여 실제 존재하는 단어만 추출
            List<DictionaryData> matchedWords = matchWordsWithDictionary(analyzedWords);

            // 5️⃣ 해당 채팅방의 단어장을 저장
            saveVocabulary(chatRoomUuid, matchedWords);
        }
    }

    /**
     * 1️⃣ 주어진 시간 범위 내의 메시지를 채팅방 UUID 기준으로 그룹핑
     */
    private Map<String, List<ChatMessageReadModel>> fetchGroupedMessagesByRoom(Instant start, Instant end) {
        List<ChatMessageReadModel> messages = chatMessageVocaService.getMessagesInRange(start, end);

        return messages.stream()
                .collect(Collectors.groupingBy(ChatMessageReadModel::getChatRoomUuid));
    }

    /**
     * 2️⃣ 하나의 채팅방 메시지를 전체 문장으로 합쳐 NLP 분석 요청
     */
    private List<AnalyzedResponseWord> analyzeMessagesAsText(String chatRoomUuid, List<ChatMessageReadModel> messages) {
        // 전체 메시지를 하나의 텍스트로 연결
        String fullText = messages.stream()
                .map(ChatMessageReadModel::getContent)
                .collect(Collectors.joining(" "));

        log.info("📌 채팅방 UUID: {}", chatRoomUuid);
        log.info("💬 분석 대상 전체 문장: {}", fullText);

        // Python 서버로 NLP 분석 요청
        List<AnalyzedResponseWord> analyzed = nlpClient.analyze(fullText);

        log.info("🧠 분석 결과 ({}개):", analyzed.size());
        for (AnalyzedResponseWord word : analyzed) {
            log.info(" - 단어: {}, 품사: {}, 언어: {}", word.getText(), word.getPos(), word.getLang());
        }

        return analyzed;
    }

    /**
     * 3️⃣ 분석된 단어 중 사전 데이터셋에 존재하는 단어만 추출
     */
    private List<DictionaryData> matchWordsWithDictionary(List<AnalyzedResponseWord> analyzedWords) {
        // ✅ 단어 + 품사 기준으로 후보 리스트 만들기
        List<WordPosPair> pairs = analyzedWords.stream()
                .map(w -> new WordPosPair(w.getText(), w.getPos()))
                .distinct()
                .toList();

        log.info("🔍 후보 단어+품사 쌍 ({}개)", pairs.size());

        // ✅ 단어 + 품사 매칭으로 MongoDB 조회
        List<DictionaryData> matchedWords = dictionaryDataRepository.findByWordAndPosPairs(pairs);

        log.info("✅ 매칭된 단어 수: {}", matchedWords.size());

        return matchedWords;
    }

    /**
     * 4️⃣ 채팅방 단어장을 MongoDB에 저장
     */
    private void saveVocabulary(String chatRoomUuid, List<DictionaryData> matchedWords) {
        // MongoDB에 저장할 단어 항목 리스트 생성
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

        // 채팅방 단어장 도큐먼트 생성
        ChatRoomVocabulary document = ChatRoomVocabulary.builder()
                .chatRoomUuid(chatRoomUuid)
                .analyzedAt(Instant.now())
                .words(wordEntries)
                .build();

        // MongoDB 저장
        chatRoomVocabularyRepository.save(document);
    }
}
