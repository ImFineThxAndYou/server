package org.example.howareyou.domain.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.quiz.dto.ClientQuizQuestion;
import org.example.howareyou.domain.quiz.dto.ClientStartResponse;
import org.example.howareyou.domain.quiz.dto.QuizQuestion;
import org.example.howareyou.domain.quiz.entity.*;
import org.example.howareyou.domain.quiz.repository.QuizResultRepository;
import org.example.howareyou.domain.quiz.repository.QuizVocaRepository;
import org.example.howareyou.domain.quiz.repository.QuizWordRepository;
import org.example.howareyou.domain.vocabulary.document.MemberVocabulary;
import org.example.howareyou.domain.quiz.dto.VocaDTO;
import org.example.howareyou.domain.vocabulary.repository.MemberVocabularyRepository;
import org.example.howareyou.domain.vocabulary.service.MemberVocaBookService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final QuizVocaRepository quizVocaRepository;
    private final MemberVocabularyRepository memberVocabularyRepository;

    /* ====================== 공개 API ====================== */

    /** 전체 랜덤 퀴즈 (레벨 필터 가능) */
    @Override
    public ClientStartResponse startRandomQuiz(String membername, QuizLevel quizLevel) {
        Long memberId = memberService.getIdByMembername(membername);

        // 최신 유니크 단어 집계 1페이지 크게 가져오기
        Page<VocaDTO> page =
                findAllWordsPaged(membername, /*lang*/ null, /*pos*/ null, 0, 1000);

        // 후보 집계 (언어 + 레벨 동시 필터)
        Map<String, VocaDTO> byWord = page.getContent().stream()
                .filter(w -> matchesLevel(quizLevel, w.getLevel()))
                .collect(Collectors.toMap(
                        e -> safe(e.getWord()), // word 기준 유니크
                        e -> e,
                        (a, b) -> a
                ));

        int unique = byWord.size();
        if (unique < 5) {
            throw new CustomException(ErrorCode.INSUFFICIENT_DISTRACTORS);
        }

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

                .map(VocaDTO::getMeaning)
                .filter(this::nonBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 문항 생성
        List<GeneratedItem> generated = buildQuestionsFromTargets(targets, null, allPool);

        QuizResult result = createAndPersistResult(memberId, QuizType.RANDOM, null, generated);
        return toClientStartResponse(result, generated);
    }

    /** 데일리 퀴즈 (레벨 필터 가능) */
    @Override
    public ClientStartResponse startDailyQuiz(String membername, LocalDate date) {
        Long memberId = memberService.getIdByMembername(membername);

        // 해당 날짜 문서의 단어(페이지 크게)
        Page<MemberVocabulary.MemberWordEntry> page =
                vocaBookService.findWordsByMemberAndDatePaged(membername, date, 0, 1000, "analyzedAt", "desc");

        if (page.isEmpty()) throw new CustomException(ErrorCode.INSUFFICIENT_DISTRACTORS);

        // 후보 집계 (언어 + 레벨 동시 필터)
        Map<String, MemberVocabulary.MemberWordEntry> byWord = page.getContent().stream()
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
                .map(MemberVocabulary.MemberWordEntry::getMeaning)
                .filter(this::nonBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 전체 보기 풀(최신 유니크에서)
        Page<VocaDTO> all = findAllWordsPaged(membername, null, null, 0, 1000);
        Set<String> allPool = all.getContent().stream()
                .map(VocaDTO::getMeaning)
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
        if (generated == null || generated.isEmpty()) { // ★ 추가
            throw new CustomException(ErrorCode.INSUFFICIENT_DISTRACTORS);
        }
        QuizResult result = QuizResult.builder()
                .memberId(memberId)
                .quizType(type)
                .dailyQuiz(dailyKeyUtc)           // RANDOM이면 null, DAILY면 UTC 자정 Instant
                .createdAt(Instant.now())
                .score(0L)
                .correctCount(0L)
                .totalQuestions((long) generated.size())
                .quizStatus(QuizStatus.PENDING)
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
                    .level(toEnumLevel(gi.level()))                 // 문자열 그대로 저장 (A1/A2 등)
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
                .quizUUID(result.getUuid())
                .quizQuestions(clientQs)
                .build();
    }

    /* ====================== 유틸 ====================== */

    private boolean nonBlank(String s) { return s != null && !s.isBlank(); }
    private String safe(String s) { return s == null ? "" : s; }
    private String nullToEmpty(String s) { return s == null ? "" : s; }

    /** 보기 언어(meaningLang) 판단 - MemberWordEntry */
    private boolean matchesMeaningLang(MemberVocabulary.MemberWordEntry w, String meaningLang) {
        String dt = safe(w.getDictionaryType()); // enko | koen | null
        String wl = safe(w.getLang());           // en | ko

        // 1) 사전 방향이 명확하면 그 방향에 맞는 meaning만 허용
        if ("enko".equalsIgnoreCase(dt)) {
            return meaningLang == null || meaningLang.isBlank() || "ko".equalsIgnoreCase(meaningLang);
        }
        if ("koen".equalsIgnoreCase(dt)) {
            return meaningLang == null || meaningLang.isBlank() || "en".equalsIgnoreCase(meaningLang);
        }

        // 2) dt가 없을 때만 기존 휴리스틱
        if ("ko".equalsIgnoreCase(meaningLang)) {
            return "en".equalsIgnoreCase(wl);
        } else { // null 또는 "en"
            return "ko".equalsIgnoreCase(wl);
        }
    }

    /** 보기 언어(meaningLang) 판단  */
    private boolean matchesMeaningLangAgg(VocaDTO w, String meaningLang) {
        String dt = safe(w.getDictionaryType());
        String wl = safe(w.getLang());

        if ("enko".equalsIgnoreCase(dt)) {
            return meaningLang == null || meaningLang.isBlank() || "ko".equalsIgnoreCase(meaningLang);
        }
        if ("koen".equalsIgnoreCase(dt)) {
            return meaningLang == null || meaningLang.isBlank() || "en".equalsIgnoreCase(meaningLang);
        }

        if ("ko".equalsIgnoreCase(meaningLang)) {
            return "en".equalsIgnoreCase(wl);
        } else {
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
    /* ---------------- quiz --------------------*/
    //최신 단어들만 뽑아서 중복없이 전체 조회 - quiz 에서 사용
    public Page<VocaDTO> findAllWordsPaged(String membername,
                                           String lang,
                                           String pos,
                                           int page,
                                           int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);

        int skip = Math.max(page, 0) * Math.max(size, 1);

        // items
        List<VocaDTO> items =
                quizVocaRepository.findWords(membername, lang, pos, skip, size);

        // total
        long total = 0L;
        List<MemberVocabularyRepository.CountOnly> cnt =
                memberVocabularyRepository.countLatestUniqueWords(membername, lang, pos);
        if (cnt != null && !cnt.isEmpty() && cnt.get(0).getTotal() != null) {
            total = cnt.get(0).getTotal();
        }

        return new PageImpl<>(items, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "analyzedAt")), total);
    }
    //퀴즈레벨 변환메서드
    // 퀴즈레벨 변환메서드: a1/a2 -> A, b1/b2 -> B, c1/c2 -> C
    private QuizLevel toEnumLevel(String level) {
        if (level == null || level.isBlank()) return null;

        String norm = level.trim()
                .toUpperCase(java.util.Locale.ROOT)
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");

        switch (norm) {
            // A 계열
            case "A1": case "A2": case "A":
                return QuizLevel.A;
            // B 계열
            case "B1": case "B2": case "B":
                return QuizLevel.B;
            // C 계열
            case "C1": case "C2": case "C":
                return QuizLevel.C;
            default:
                log.warn("Unknown quiz level '{}', storing as null.", level);
                return null;
        }
    }
}