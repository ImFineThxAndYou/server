package org.example.howareyou.domain.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.quiz.dto.ClientQuizQuestion;
import org.example.howareyou.domain.quiz.dto.ClientStartResponse;
import org.example.howareyou.domain.quiz.dto.QuizQuestion;
import org.example.howareyou.domain.quiz.dto.QuizWordCreate;
import org.example.howareyou.domain.quiz.entity.QuizResult;
import org.example.howareyou.domain.quiz.entity.QuizType;
import org.example.howareyou.domain.quiz.entity.QuizWord;
import org.example.howareyou.domain.quiz.repository.QuizResultRepository;
import org.example.howareyou.domain.quiz.repository.QuizWordRepository;
import org.example.howareyou.domain.vocabulary.document.MemberVocabulary;
import org.example.howareyou.domain.vocabulary.service.MemberVocaBookService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.example.howareyou.global.exception.ErrorCode.NORETRY;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class QuizGeneratorServiceImpl implements QuizGeneratorService {

    private static final SecureRandom RND = new SecureRandom();

    private final MemberService memberService;               // membername -> memberId 변환용
    private final MemberVocaBookService vocaBookService;     // 단어장 조회
    private final QuizResultRepository quizResultRepository; // 퀴즈 결과 저장
    private final QuizWordRepository quizWordRepository;     // 문항 저장

    // ====================== 공개 API ======================

    /** 랜덤 퀴즈: membername + 보기언어(language=meaningLang)만 받음, 문항수 자동(5~30) */
    @Override
    public ClientStartResponse startRandomQuiz(String membername, String language /*= meaningLang*/) {
        // 내부 저장/시도 제한은 memberId 기준
        Long memberId = memberService.getIdByMembername(membername);

        // 1) 전체 문서에서 후보(원문) 개수 집계
        List<MemberVocabulary> docs = safeList(vocaBookService.findByMembername(membername));
        int unique = (int) docs.stream()
                .flatMap(d -> d.getWords().stream())
                .filter(w -> matchesMeaningLang(w, language))
                .map(MemberVocabulary.MemberWordEntry::getWord)
                .filter(this::nonBlank)
                .distinct()
                .count();

        if (unique < 5) throw new CustomException(ErrorCode.INSUFFICIENT_DISTRACTORS);

        // 2) 문항 수 자동 결정 (최소 5, 최대 30, 후보 수 초과 금지)
        int count = Math.min(30, Math.max(5, unique));

        // 3) 타깃/문항 생성
        List<Target> targets = collectAllTargets(membername, language, count);
        List<QuizQuestion> questions = buildQuestionsFromTargets(
                targets,
                null,
                collectAllMeanings(membername, language)
        );

        // 4) 원본 생성/시도 제한/저장 (모두 memberId 기준)
        QuizResult original = findOrCreateOriginal(memberId, QuizType.RANDOM, null);
        enforceAttemptLimit(original.getId());
        persistOriginalIfNew(original, QuizType.RANDOM, null, questions, memberId);

        // 5) 응답
        return toClientStartResponse(original, questions);
    }

    /** 데일리 퀴즈: membername + date + 보기언어(language=meaningLang), 문항수 자동(5~30) */
    @Override
    public ClientStartResponse startDailyQuiz(String membername, LocalDate date, String language /*= meaningLang*/) {
        Long memberId = memberService.getIdByMembername(membername);

        MemberVocabulary doc = vocaBookService.findByMembernameAndDate(membername, date);
        if (doc == null || doc.getWords() == null) throw new CustomException(ErrorCode.INSUFFICIENT_DISTRACTORS);

        int unique = (int) doc.getWords().stream()
                .filter(w -> matchesMeaningLang(w, language))
                .map(MemberVocabulary.MemberWordEntry::getWord)
                .filter(this::nonBlank)
                .distinct()
                .count();

        if (unique < 5) throw new CustomException(ErrorCode.INSUFFICIENT_DISTRACTORS);

        int count = Math.min(30, Math.max(5, unique));

        List<Target> targets  = collectDailyTargets(membername, date, language, count);
        Set<String> dailyPool = collectDailyMeanings(membername, date, language);
        Set<String> allPool   = collectAllMeanings(membername, language);

        List<QuizQuestion> questions = buildQuestionsFromTargets(targets, dailyPool, allPool);

        QuizResult original = findOrCreateOriginal(memberId, QuizType.DAILY, null /* 필요하면 날짜 키 사용 */);
        enforceAttemptLimit(original.getId());
        persistOriginalIfNew(original, QuizType.DAILY, null, questions, memberId);

        return toClientStartResponse(original, questions);
    }

    // ====================== 내부 로직 ======================

    // 타깃 한 건(문항의 질문에 쓰일 word + 정답 meaning)
    private record Target(String word, String meaning) {}

    // 해당 날짜 문서에서 lang 일치하는 word만 유니크 추출
    private List<Target> collectDailyTargets(String membername, LocalDate date, String meaningLang, int count) {
        MemberVocabulary doc = vocaBookService.findByMembernameAndDate(membername, date);
        if (doc == null || doc.getWords() == null) {
            throw new CustomException(ErrorCode.INSUFFICIENT_DISTRACTORS);
        }

        Map<String, String> byWord = doc.getWords().stream()
                .filter(w -> matchesMeaningLang(w, meaningLang))
                .collect(Collectors.toMap(
                        MemberVocabulary.MemberWordEntry::getWord,
                        MemberVocabulary.MemberWordEntry::getMeaning,
                        (a, b) -> a
                ));

        List<Target> list = byWord.entrySet().stream()
                .map(e -> new Target(e.getKey(), e.getValue()))
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.shuffle(list, RND);
        if (list.size() < count) throw new CustomException(ErrorCode.INSUFFICIENT_DISTRACTORS);
        return list.subList(0, count);
    }

    // 전체 문서에서 lang 일치하는 word만 유니크 추출
    private List<Target> collectAllTargets(String membername, String meaningLang, int count) {
        List<MemberVocabulary> docs = safeList(vocaBookService.findByMembername(membername));

        Map<String, String> byWord = docs.stream()
                .flatMap(d -> d.getWords().stream())
                .filter(w -> matchesMeaningLang(w, meaningLang))
                .collect(Collectors.toMap(
                        MemberVocabulary.MemberWordEntry::getWord,
                        MemberVocabulary.MemberWordEntry::getMeaning,
                        (a, b) -> a
                ));

        List<Target> list = byWord.entrySet().stream()
                .map(e -> new Target(e.getKey(), e.getValue()))
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.shuffle(list, RND);
        if (list.size() < count) throw new CustomException(ErrorCode.INSUFFICIENT_DISTRACTORS);
        return list.subList(0, count);
    }

    // 해당 날짜 의미 풀 수집 (meaning만)
    private Set<String> collectDailyMeanings(String membername, LocalDate date, String meaningLang) {
        try {
            MemberVocabulary doc = vocaBookService.findByMembernameAndDate(membername, date);
            if (doc == null || doc.getWords() == null) return Collections.emptySet();
            return doc.getWords().stream()
                    .filter(w -> matchesMeaningLang(w, meaningLang))
                    .map(MemberVocabulary.MemberWordEntry::getMeaning)
                    .filter(this::nonBlank)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (Exception e) {
            log.debug("Daily vocabulary not found for {} on {}: {}", membername, date, e.getMessage());
            return Collections.emptySet();
        }
    }

    // 전체 의미 풀 수집 (meaning만)
    private Set<String> collectAllMeanings(String membername, String meaningLang) {
        List<MemberVocabulary> docs = safeList(vocaBookService.findByMembername(membername));
        return docs.stream()
                .flatMap(doc -> doc.getWords().stream())
                .filter(w -> matchesMeaningLang(w, meaningLang))
                .map(MemberVocabulary.MemberWordEntry::getMeaning)
                .filter(this::nonBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // 타깃 리스트로부터 실제 4지선다 문항 만들기
    private List<QuizQuestion> buildQuestionsFromTargets(List<Target> targets,
                                                         Set<String> dailyPoolOrNull,
                                                         Set<String> allPool) {
        List<QuizQuestion> out = new ArrayList<>(targets.size());

        for (Target t : targets) {
            String answer = t.meaning();
            List<String> choices = new ArrayList<>(4);
            choices.add(answer);

            // 우선순위: 데일리 풀(있다면) → 전체 풀
            List<String> candidates = new ArrayList<>();
            if (dailyPoolOrNull != null) {
                candidates.addAll(dailyPoolOrNull);
            }
            candidates.addAll(allPool);

            // 정답/중복/공백 제거
            Set<String> used = new HashSet<>(choices);
            List<String> filtered = candidates.stream()
                    .filter(this::nonBlank)
                    .filter(m -> !used.contains(m))
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));

            if (filtered.size() < 3) {
                throw new CustomException(ErrorCode.INSUFFICIENT_DISTRACTORS);
            }

            Collections.shuffle(filtered, RND);
            choices.add(filtered.get(0));
            choices.add(filtered.get(1));
            choices.add(filtered.get(2));

            Collections.shuffle(choices, RND);

            out.add(QuizQuestion.builder()
                    .question(t.word())
                    .choices(choices)
                    .answerIndex(choices.indexOf(answer))
                    .build());
        }
        return out;
    }

    // ====================== 공용 유틸/저장 ======================

    private List<MemberVocabulary> safeList(List<MemberVocabulary> in) {
        return in == null ? Collections.emptyList() : in;
    }

    private boolean nonBlank(String s) { return s != null && !s.isBlank(); }

    private String safe(String s) { return s == null ? "" : s; }

    /** 원본 퀴즈 찾기/생성은 memberId 기준(엔티티가 memberId를 가짐) */
    private QuizResult findOrCreateOriginal(Long memberId, QuizType type, Instant dailyKeyUtc) {
        Optional<QuizResult> found;

        if (type == QuizType.RANDOM) {
            found = quizResultRepository.findLatestOriginalRandom(memberId, QuizType.RANDOM);
        } else { // DAILY
            if (dailyKeyUtc == null) {
                // 지금 구조가 dailyKeyUtc를 null로 두는 설계라면 이 메서드 사용
                found = quizResultRepository.findLatestOriginalDailyIsNull(memberId, QuizType.DAILY);
            } else {
                found = quizResultRepository.findLatestOriginalDaily(memberId, QuizType.DAILY, dailyKeyUtc);
            }
        }

        return found.orElseGet(() -> QuizResult.builder()
                .memberId(memberId)
                .quizType(type)
                .dailyQuiz(dailyKeyUtc)   // null이면 null로 저장
                .isRequiz(Boolean.FALSE)
                .createdAt(Instant.now())
                .quiz_count(0)
                .build());
    }

    /** 시도 횟수 제한(5회) — memberId + originalId 기준 */
    private void enforceAttemptLimit(Long originalId) {
        if (originalId == null) return;
        long attempt = quizResultRepository.countAttempts(originalId);
        if (attempt >= 5) throw new CustomException(ErrorCode.NORETRY);
    }

    /** original 신규 저장 및 QuizWord 배치 저장, 시도수 업데이트 — memberId 필요 */
    private void persistOriginalIfNew(QuizResult original,
                                      QuizType type,
                                      Instant dailyKeyUtc,
                                      List<QuizQuestion> questions,
                                      Long memberId) {
        boolean isNew = (original.getId() == null);
        if (isNew) {
            original.setScore(0L);
            original.setTotalQuestions((long) questions.size());
            original.setCorrectCount(0L);
            original.setCreatedAt(Instant.now());
            original.setIsRequiz(Boolean.FALSE);
            original = quizResultRepository.save(original);
            saveQuizWords(original, questions);
        }

        long attempt = quizResultRepository.countAttempts(original.getId());
        original.setQuiz_count((int) attempt);
        quizResultRepository.save(original);
    }

    private ClientStartResponse toClientStartResponse(QuizResult original, List<QuizQuestion> qs) {
        List<ClientQuizQuestion> clientQs = new ArrayList<>(qs.size());
        for (int i = 0; i < qs.size(); i++) {
            QuizQuestion q = qs.get(i);
            clientQs.add(ClientQuizQuestion.builder()
                    .question(q.getQuestion())
                    .choices(q.getChoices())
                    .questionNo(i + 1)
                    .build());
        }

        Integer count = original.getQuiz_count();
        int used = (count == null) ? 1 : count;
        int max  = 5;

        return ClientStartResponse.builder()
                .quizResultId(original.getId())
                .quizQuestions(clientQs)
                .maxRetry(max)
                .usedRetry(used)
                .remainingRetry(Math.max(0, max - used))
                .build();
    }

    /** 보기 언어(meaningLang) 판단: ko(보기=한글) → 원문은 en, en(보기=영어) → 원문은 ko */
    private boolean matchesMeaningLang(MemberVocabulary.MemberWordEntry w, String meaningLang) {
        String dt = safe(w.getDictionaryType()); // "enko" or "koen" or null
        String wl = safe(w.getLang());           // "en" or "ko"

        if ("ko".equalsIgnoreCase(meaningLang)) {
            // 보기=한글 → 원문은 영어(en)이어야 함
            if ("enko".equalsIgnoreCase(dt)) return true;   // 명시적 방향
            return "en".equalsIgnoreCase(wl);
        } else { // meaningLang = "en"
            // 보기=영어 → 원문은 한국어(ko)여야 함
            if ("koen".equalsIgnoreCase(dt)) return true;
            return "ko".equalsIgnoreCase(wl);
        }
    }

    /* QuizWord 배치 저장 */
    private void saveQuizWords(QuizResult original, List<QuizQuestion> questions) {
        List<QuizWordCreate> creates = new ArrayList<>(questions.size());

        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);
            List<String> choices = q.getChoices();
            int answerIndex = q.getAnswerIndex();

            if (choices == null || choices.size() < 4)
                throw new IllegalStateException("퀴즈 보기가 4개 미만입니다.");
            if (answerIndex < 0 || answerIndex >= choices.size())
                throw new CustomException(ErrorCode.INVALID_SELECTION_INDEX);

            creates.add(QuizWordCreate.builder()
                    .word(q.getQuestion())
                    .choices(List.of(choices.get(0), choices.get(1), choices.get(2), choices.get(3)))
                    .answerIndex(answerIndex)
                    .questionNo(i + 1)
                    .meaning(choices.get(answerIndex))
                    .build());
        }

        List<QuizWord> batch = new ArrayList<>(creates.size());
        for (QuizWordCreate c : creates) {
            batch.add(QuizWord.builder()
                    .quizResult(original)
                    .questionNo(c.getQuestionNo())
                    .word(c.getWord())
                    .meaning(c.getMeaning())
                    .choice1(c.getChoices().get(0))
                    .choice2(c.getChoices().get(1))
                    .choice3(c.getChoices().get(2))
                    .choice4(c.getChoices().get(3))
                    .correctAnswer(c.getAnswerIndex() + 1)   // 1~4 저장
                    .userAnswer(null)
                    .isCorrect(null)
                    .createdAt(Instant.now())
                    .build());
        }

        quizWordRepository.saveAll(batch);
    }
}