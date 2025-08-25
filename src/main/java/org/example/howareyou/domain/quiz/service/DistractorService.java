package org.example.howareyou.domain.quiz.service;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.vocabulary.document.DictionaryData;
import org.example.howareyou.domain.vocabulary.dto.AggregatedWordEntry;
import org.example.howareyou.domain.vocabulary.repository.DictionaryDataRepository;
import org.example.howareyou.domain.vocabulary.service.MemberVocaBookService;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 퀴즈가 생성될때 오답풀을 dictionary 에서 생성하여
 * INSUFFICIENT_DISTRACTORS(HttpStatus.UNPROCESSABLE_ENTITY, "Q002", "오답 선택지가 부족합니다."),
 * 라는 오류 발생률을 줄여줍니다. */
@Service
@RequiredArgsConstructor
public class DistractorService {
    private final DictionaryDataRepository dictionaryDataRepository;
    private final MemberVocaBookService memberVocabularyService;
    private final MemberService memberService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final SecureRandom RND = new SecureRandom();
    private static final String DICT_CACHE_KEY = "quiz:distractors:bcPool";

    /**
     * 퀴즈 오답 선택지 생성
     */
    public List<String> pickDistractors(String correctWord,
                                        Set<String> userAnswerPool,
                                        int totalQuestions,
                                        String membername) {

        //사전에서 BC 단어 오답풀 가져오기
        String lang = memberService.getMemberByMembername(membername).getProfile().getLanguage();
        String cacheKey = DICT_CACHE_KEY + ":" + lang;

        // 강제캐스팅 경고 무시하기위한 어노테이션
        @SuppressWarnings("unchecked")
        List<String> bcPool = (List<String>) redisTemplate.opsForValue().get(cacheKey);

        if (bcPool == null || bcPool.isEmpty()) {
            bcPool = dictionaryDataRepository.findByLevel(List.of("b1", "b2", "c1", "c2"), lang)
                    .stream()
                    .map(DictionaryData::getWord)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            redisTemplate.opsForValue().set(cacheKey, bcPool);
        }

        //  Dictionary 에서 가져오기
        List<String> available = bcPool.stream()
                .filter(w -> !userAnswerPool.contains(w))
                .filter(w -> !w.equalsIgnoreCase(correctWord))
                .collect(Collectors.toList());

        // 오답 후보가 부족할 경우 사용자 단어장에서 보충
        if (available.size() < 3) {
            Page<AggregatedWordEntry> userPage =
                    memberVocabularyService.findLatestUniqueWordsPaged(membername, null, null, 0, 1000);

            List<String> userWords = userPage.getContent().stream()
                    .map(AggregatedWordEntry::getWord)
                    .filter(Objects::nonNull)
                    .filter(w -> !userAnswerPool.contains(w))
                    .filter(w -> !w.equalsIgnoreCase(correctWord))
                    .collect(Collectors.toList());

            available.addAll(userWords);
            available = available.stream().distinct().collect(Collectors.toList());
        }

        // 랜덤 인덱스 3개 선택(오답선택지)
        return pickRandom(available, 3);
    }

    /**
     * 주어진 리스트에서 랜덤하게 limit 개 추출
     */
    private List<String> pickRandom(List<String> pool, int limit) {
        if (pool.size() <= limit) {
            return new ArrayList<>(pool);
        }

        Set<Integer> picked = new HashSet<>();
        List<String> result = new ArrayList<>(limit);

        while (result.size() < limit) {
            int idx = RND.nextInt(pool.size());
            if (picked.add(idx)) {
                result.add(pool.get(idx));
            }
        }
        return result;
    }
}