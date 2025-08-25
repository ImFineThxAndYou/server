package org.example.howareyou.domain.quiz.service;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.vocabulary.document.DictionaryData;
import org.example.howareyou.domain.vocabulary.dto.AggregatedWordEntry;
import org.example.howareyou.domain.vocabulary.repository.DictionaryDataRepository;
import org.example.howareyou.domain.vocabulary.service.MemberVocaBookService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

    private static final SecureRandom RND = new SecureRandom();

    public List<String> pickDistractors(String correctWord,
                                        Set<String> userAnswerPool,
                                        int totalQuestions,
                                        String membername) {

        //  B, C 레벨 오답 후보 불러오기
        String lang = memberService.getMemberByMembername(membername).getProfile().getLanguage(); // TODO: 더 좋은방식있으면 바꿀예정
        List<DictionaryData> bcWords = dictionaryDataRepository
                .findByLevel(List.of("b1","b2","c1","c2"),lang );
        // 오답 후보풀 생성
        List<String> available = bcWords.stream()
                .map(DictionaryData::getWord)
                .filter(w -> !userAnswerPool.contains(w))
                .filter(w -> !w.equalsIgnoreCase(correctWord))
                .distinct()
                .collect(Collectors.toList());

        // 오답선택지 3개미만? 에러
        if (available.size() < 3) {
            Page<AggregatedWordEntry> userPage =
                    memberVocabularyService.findLatestUniqueWordsPaged(membername, null, null, 0, 1000);

            List<String> userWords = userPage.getContent().stream()
                    .map(AggregatedWordEntry::getWord)
                    .filter(Objects::nonNull)
                    .filter(w -> !userAnswerPool.contains(w))
                    .filter(w -> !w.equalsIgnoreCase(correctWord))
                    .toList();

            available.addAll(userWords);
            available = available.stream().distinct().toList();
        }


        //  랜덤 3개 리턴
        Collections.shuffle(available, RND);
        return available.stream().limit(3).toList();
    }
}
