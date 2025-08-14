package org.example.howareyou.domain.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.quiz.dto.ClientQuizQuestion;
import org.example.howareyou.domain.quiz.dto.ClientStartResponse;
import org.example.howareyou.domain.quiz.dto.QuizQuestion;
import org.example.howareyou.domain.quiz.entity.QuizLevel;
import org.example.howareyou.domain.quiz.entity.QuizResult;
import org.example.howareyou.domain.quiz.entity.QuizType;
import org.example.howareyou.domain.quiz.entity.QuizWord;
import org.example.howareyou.domain.quiz.repository.QuizResultRepository;
import org.example.howareyou.domain.quiz.repository.QuizWordRepository;
import org.example.howareyou.domain.vocabulary.document.MemberVocabulary;
import org.example.howareyou.domain.vocabulary.dto.AggregatedWordEntry;
import org.example.howareyou.domain.vocabulary.quiz.VocaDTO;
import org.example.howareyou.domain.vocabulary.service.MemberVocaBookService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class QuizGeneratorServiceImpl implements QuizGeneratorService {

    private static final SecureRandom RND = new SecureRandom();

    private final MemberService memberService;
    private final MemberVocaBookService vocaBookService;
    private final QuizResultRepository quizResultRepository;
    private final QuizWordRepository quizWordRepository;

    /* ====================== 공개 API ====================== */

    /** 전체 랜덤 퀴즈 (레벨 필터 가능) */
    @Override
    public ClientStartResponse startRandomQuiz(String membername, String language, QuizLevel quizLevel) {
        Long memberId = memberService.getIdByMembername(membername);

        // 최신 유니크 단어 집계 1페이지 크게 가져오기
        Page<VocaDTO> page =
                vocaBookService.findAllWordsPaged(membername, /*lang*/ null, /*pos*/ null, 0, 1000);

        // 후보 집계 (언어 + 레벨 동시 필터)
        Map<String, AggregatedWordEntry> byWord = page.getContent().stream()
                .filter(w -> matchesMeaningLangAgg(w, language))
                .filter(w -> matchesLevel(quizLevel, w.getLevel()))
                .collect(Collectors.toMap(
                        e -> safe(e.getWord()), // word 기준 유니크
                        e -> e,
                        (a, b) -> a
                ));

        int unique = byWord.size();
        if (unique < 5) throw new CustomException(ErrorCode.INSUFFICIENT_DISTRACTORS);

        int count = pickQuestionCount(unique);

        // 타깃 만들기
        List<Target> targets = byWord.values().stream()
                .map(e -> new Target(
                        e.getWord(),
                        e.getMeaning(),
                        nullToEmpty(e.getLevel()),
                        nullToEmpty(e.getPos())
                ))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(targets, RND);
        targets = targets.subList(0, count);

        // 보기 풀(의미) 수집
        Set<String> allPool = page.getContent().stream()
                .filter(w -> matchesMeaningLangAgg(w, language))
                .map(AggregatedWordEntry::getMeaning)
                .filter(this::nonBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 문항 생성
        List<GeneratedItem> generated = buildQuestionsFromTargets(targets, null, allPool);

        QuizResult result = createAndPersistResult(memberId, QuizType.RANDOM, null, generated);
        return toClientStartResponse(result, generated);
    }

    /** 데일리 퀴즈 (레벨 필터 가능) */
    @Override
    public ClientStartResponse startDailyQuiz(String membername, LocalDate date, String language, QuizLevel quizLevel) {
        Long memberId = memberService.getIdByMembername(membername);

        // 해당 날짜 문서의 단어(페이지 크게)
        Page<MemberVocabulary.MemberWordEntry> page =
                vocaBookService.findWordsByMemberAndDatePaged(membername, date, 0, 1000, "analyzedAt", "desc");

        if (page.isEmpty()) throw new CustomException(ErrorCode.INSUFFICIENT_DISTRACTORS);

        // 후보 집계 (언어 + 레벨 동시 필터)
        Map<String, MemberVocabulary.MemberWordEntry> byWord = page.getContent().stream()
                .filter(w -> matchesMeaningLang(w, language))
                .filter(w -> matchesLevel(quizLevel, w.getLevel()))
                .collect(Collectors.toMap(
                        MemberVocabulary.MemberWordEntry::getWord,
                        w -> w,
                        (a, b) -> a
                ));

        int unique = byWord.size();
        if (unique < 5) throw new CustomException(ErrorCode.INSUFFICIENT_DISTRACTORS);

        int count = pickQuestionCount(unique);

        List<Target> targets = byWord.values().stream()
                .map(w -> new Target(
                        w.getWord(),
                        w.getMeaning(),
                        nullToEmpty(w.getLevel()),
                        nullToEmpty(w.getPos())
                ))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(targets, RND);
        targets = targets.subList(0, count);

        // 당일 보기 풀
        Set<String> dailyPool = page.getContent().stream()
                .filter(w -> matchesMeaningLang(w, language))
                .map(MemberVocabulary.MemberWordEntry::getMeaning)
                .filter(this::nonBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 전체 보기 풀(최신 유니크에서)
        Page<AggregatedWordEntry> all = vocaBookService.findLatestUniqueWordsPaged(membername, null, null, 0, 1000);
        Set<String> allPool = all.getContent().stream()
                .filter(w -> matchesMeaningLangAgg(w, language))
                .map(AggregatedWordEntry::getMeaning)
                .filter(this::nonBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<GeneratedItem> generated = buildQuestionsFromTargets(targets, dailyPool, allPool);

        Instant dailyKeyUtc = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        QuizResult result = createAndPersistResult(memberId, QuizType.DAILY, dailyKeyUtc, generated);
        return toClientStartResponse(result, generated);
    }

    /* ====================== 내부 로직 ====================== */

    /** 타깃(정답 쌍) + 메타정보 */
    private record Target(String word, String meaning, String level, String pos) {}

    /** 생성 결과 한 문항(클라이언트용 문항 + 메타(level,pos) 함께 보관) */
    private record GeneratedItem(QuizQuestion question, String level, String pos) {}

    /** 4지선다 생성 */
    private List<GeneratedItem> buildQuestionsFromTargets(List<Target> targets,
                                                          Set<String> dailyPoolOrNull,
                                                          Set<String> allPool) {
        List<GeneratedItem> out = new ArrayList<>(targets.size());

        for (Target t : targets) {
            String answer = t.meaning();
            List<String> choices = new ArrayList<>(4);
            choices.add(answer);

            List<String> candidates = new ArrayList<>();
            if (dailyPoolOrNull != null) candidates.addAll(dailyPoolOrNull);
            candidates.addAll(allPool);

            Set<String> used = new HashSet<>(choices);
            List<String> filtered = candidates.stream()
                    .filter(this::nonBlank)
                    .filter(m -> !used.contains(m))
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));

            if (filtered.size() < 3) throw new CustomException(ErrorCode.INSUFFICIENT_DISTRACTORS);

            Collections.shuffle(filtered, RND);
            choices.add(filtered.get(0));
            choices.add(filtered.get(1));
            choices.add(filtered.get(2));
            Collections.shuffle(choices, RND);

            QuizQuestion qq = QuizQuestion.builder()
                    .question(t.word())
                    .choices(choices)
                    .answerIndex(choices.indexOf(answer))
                    .build();

            out.add(new GeneratedItem(qq, t.level(), t.pos()));
        }
        return out;
    }

    /* ====================== 저장 & 응답 ====================== */

    private QuizResult createAndPersistResult(Long memberId,
                                              QuizType type,
                                              Instant dailyKeyUtc,
                                              List<GeneratedItem> generated) {
        QuizResult result = QuizResult.builder()
                .memberId(memberId)
                .quizType(type)
                .dailyQuiz(dailyKeyUtc)           // RANDOM이면 null, DAILY면 UTC 자정 Instant
                .createdAt(Instant.now())
                .score(0L)
                .correctCount(0L)
                .totalQuestions((long) generated.size())
                .build();

        result = quizResultRepository.save(result);
        saveQuizWords(result, generated);
        return result;
    }

    private void saveQuizWords(QuizResult result, List<GeneratedItem> generated) {
        List<QuizWord> batch = new ArrayList<>(generated.size());

        for (int i = 0; i < generated.size(); i++) {
            GeneratedItem gi = generated.get(i);
            QuizQuestion q = gi.question();

            List<String> choices = q.getChoices();
            int answerIndex = q.getAnswerIndex();
            if (choices == null || choices.size() < 4)
                throw new IllegalStateException("퀴즈 보기가 4개 미만입니다.");
            if (answerIndex < 0 || answerIndex >= choices.size())
                throw new CustomException(ErrorCode.INVALID_SELECTION_INDEX);

            batch.add(QuizWord.builder()
                    .quizResult(result)
                    .questionNo(i + 1)
                    .word(q.getQuestion())
                    .meaning(choices.get(answerIndex))
                    .choice1(choices.get(0))
                    .choice2(choices.get(1))
                    .choice3(choices.get(2))
                    .choice4(choices.get(3))
                    .correctAnswer(answerIndex + 1)     // 1~4
                    .level(QuizLevel.valueOf(gi.level()))                  // 문자열 그대로 저장 (A1/A2 등)
                    .pos(gi.pos())
                    .createdAt(Instant.now())
                    .build());
        }

        quizWordRepository.saveAll(batch);
    }

    private ClientStartResponse toClientStartResponse(QuizResult result, List<GeneratedItem> generated) {
        List<ClientQuizQuestion> clientQs = new ArrayList<>(generated.size());
        for (int i = 0; i < generated.size(); i++) {
            QuizQuestion q = generated.get(i).question();
            clientQs.add(ClientQuizQuestion.builder()
                    .question(q.getQuestion())
                    .choices(q.getChoices())
                    .questionNo(i + 1)
                    .build());
        }

        return ClientStartResponse.builder()
                .quizResultId(result.getId())
                .quizQuestions(clientQs)
                .build();
    }

    /* ====================== 유틸 ====================== */

    private boolean nonBlank(String s) { return s != null && !s.isBlank(); }
    private String safe(String s) { return s == null ? "" : s; }
    private String nullToEmpty(String s) { return s == null ? "" : s; }

    /** 보기 언어(meaningLang) 판단 - MemberWordEntry */
    private boolean matchesMeaningLang(MemberVocabulary.MemberWordEntry w, String meaningLang) {
        String dt = safe(w.getDictionaryType()); // "enko" or "koen" or null
        String wl = safe(w.getLang());           // "en" or "ko"
        if ("ko".equalsIgnoreCase(meaningLang)) {
            if ("enko".equalsIgnoreCase(dt)) return true;
            return "en".equalsIgnoreCase(wl);
        } else {
            if ("koen".equalsIgnoreCase(dt)) return true;
            return "ko".equalsIgnoreCase(wl);
        }
    }

    /** 보기 언어(meaningLang) 판단  */
    private boolean matchesMeaningLangAgg(VocaDTO w, String meaningLang) {
        String dt = safe(w.getDictionaryType());
        String wl = safe(w.getLang());
        if ("ko".equalsIgnoreCase(meaningLang)) {
            if ("enko".equalsIgnoreCase(dt)) return true;
            return "en".equalsIgnoreCase(wl);
        } else {
            if ("koen".equalsIgnoreCase(dt)) return true;
            return "ko".equalsIgnoreCase(wl);
        }
    }

    /** 레벨 필터(quizLevel == null 이면 전체 허용) */
    private boolean matchesLevel(QuizLevel ql, String levelRow) {
        return ql == null || ql.match(levelRow);
    }

    /** 문항수 조절 */
    private int pickQuestionCount(int unique) {
        int max = Math.min(30, unique);
        int min = Math.min(5, max); // unique < 5는 호출 전 예외 처리됨
        return min + RND.nextInt(max - min + 1);
    }
}