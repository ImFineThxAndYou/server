package org.example.howareyou.domain.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.entity.Member;
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

    private final MemberService memberService;               // id -> membername
    private final MemberVocaBookService vocaBookService;     // 단어장 조회
    private final QuizResultRepository quizResultRepository; // 퀴즈 결과 저장
    private final QuizWordRepository quizWordRepository; // 만든 퀴즈 저장

    // ====================== 공개 API (배치 전용) ======================

    @Override
    public ClientStartResponse startRandomQuiz(Long memberId, String meaningLang, int count) {
        validateCount(count);

        String membername = getMembernameById(memberId);

        // 1) 타깃 word 뽑기(유니크)
        List<Target> targets = collectAllTargets(membername, meaningLang, count);

        // 2) 문항 생성
        List<QuizQuestion> questions = buildQuestionsFromTargets(
                targets,
                /*dailyPool*/ null,
                /*allPool*/ collectAllMeanings(membername, meaningLang)
        );

        // 3) 원본 생성/시도 제한/저장
        QuizResult original = findOrCreateOriginal(memberId, QuizType.RANDOM, null);
        enforceAttemptLimit(memberId, original.getId());
        persistOriginalIfNew(original, QuizType.RANDOM, null, questions, memberId);

        // 4) 응답 매핑
        return toClientStartResponse(original, questions);
    }

    @Override
    public ClientStartResponse startDailyQuiz(Long memberId, LocalDate date, String meaningLang, int count) {
        validateCount(count);

        String membername = getMembernameById(memberId);

        // 1) 타깃 word 뽑기(유니크) — 해당 날짜 문서에서만
        List<Target> targets = collectDailyTargets(membername, date, meaningLang, count);

        // 2) 해당 날짜 의미 풀 + 전체 의미 풀
        Set<String> dailyPool = collectDailyMeanings(membername, date, meaningLang);
        Set<String> allPool   = collectAllMeanings(membername, meaningLang);

        // 3) 문항 생성(부족 시 전체 보강)
        List<QuizQuestion> questions = buildQuestionsFromTargets(targets, dailyPool, allPool);

        // 4) 원본 생성/시도 제한/저장
        // (필요하면 dailyKeyUtc 넣어도 됨. 현 구조에선 null 유지)
        QuizResult original = findOrCreateOriginal(memberId, QuizType.DAILY, null);
        enforceAttemptLimit(memberId, original.getId());
        persistOriginalIfNew(original, QuizType.DAILY, null, questions, memberId);

        // 5) 응답 매핑
        return toClientStartResponse(original, questions);
    }

    // ====================== 내부 로직 ======================

    private void validateCount(int count) {
        if (count < 5 || count > 30) {
            throw new CustomException(ErrorCode.INSUFFICIENT_DISTRACTORS);
        }
    }

    // 타깃 한 건(문항의 질문에 쓰일 word + 정답 meaning)
    private record Target(String word, String meaning) {}

    // 해당 날짜 문서에서 lang 일치하는 word만 유니크 추출
    private List<Target> collectDailyTargets(String membername, LocalDate date, String meaningLang, int count) {
        MemberVocabulary doc = vocaBookService.findByMembernameAndDate(membername, date);
        Map<String, String> byWord = doc.getWords().stream()
                .filter(w -> meaningLang.equalsIgnoreCase(safe(w.getLang())))
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
        List<MemberVocabulary> docs = vocaBookService.findByMembername(membername);
        Map<String, String> byWord = docs.stream()
                .flatMap(d -> d.getWords().stream())
                .filter(w -> meaningLang.equalsIgnoreCase(safe(w.getLang())))
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
            return doc.getWords().stream()
                    .filter(w -> meaningLang.equalsIgnoreCase(safe(w.getLang())))
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
        List<MemberVocabulary> docs = vocaBookService.findByMembername(membername);
        if (docs == null || docs.isEmpty()) return Collections.emptySet();

        return docs.stream()
                .flatMap(doc -> doc.getWords().stream())
                .filter(w -> meaningLang.equalsIgnoreCase(safe(w.getLang())))
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

    private String getMembernameById(Long memberId) {
        Member m = memberService.getMemberById(memberId);
        String membername = m.getMembername();
        if (membername == null || membername.isBlank()) {
            // 별도 에러코드 안 쓰기로 하셨으니 BAD_REQUEST 계열로 재사용
            throw new CustomException(ErrorCode.INSUFFICIENT_DISTRACTORS);
        }
        return membername;
    }

    private boolean nonBlank(String s) { return s != null && !s.isBlank(); }

    private String safe(String s) { return s == null ? "" : s; }

    private QuizResult findOrCreateOriginal(Long memberId, QuizType type, Instant dailyKeyUtc) {
        Optional<QuizResult> found = (type == QuizType.RANDOM)
                ? quizResultRepository.findLatestOriginal(memberId, QuizType.RANDOM, null)
                : quizResultRepository.findLatestOriginal(memberId, QuizType.DAILY, dailyKeyUtc);

        return found.orElseGet(() -> QuizResult.builder()
                .memberId(memberId)
                .quizType(type)
                .dailyQuiz(dailyKeyUtc)
                .isRequiz(Boolean.FALSE)
                .createdAt(Instant.now())
                .quiz_count(0)
                .build());
    }

    private void enforceAttemptLimit(Long memberId, Long originalId) {
        if (originalId == null) return;
        long attempt = quizResultRepository.countAttempts(memberId, originalId);
        if (attempt >= 5) {
            throw new CustomException(NORETRY);
        }
    }

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

            saveQuizWords(original,questions);
        }

        long attempt = quizResultRepository.countAttempts(memberId, original.getId());
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

    private void saveQuizWords(QuizResult original, List<QuizQuestion> questions) {
        // 1) QuizQuestion -> 내부 DTO(QuizWordCreate)로 정규화
        List<QuizWordCreate> creates = new ArrayList<>(questions.size());

        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);

            String word = q.getQuestion();
            List<String> choices = q.getChoices();
            int answerIndex = q.getAnswerIndex();

            // 방어 로직
            if (choices == null || choices.size() < 4) {
                throw new IllegalStateException("퀴즈 보기가 4개 미만입니다.");
            }
            if (answerIndex < 0 || answerIndex >= choices.size()) {
                throw new CustomException(ErrorCode.INVALID_SELECTION_INDEX);
            }

            creates.add(QuizWordCreate.builder()
                    .word(word)
                    // 보기 4개만 보존 (혹시 4개 초과가 올 수도 있으니 0~3만)
                    .choices(List.of(choices.get(0), choices.get(1), choices.get(2), choices.get(3)))
                    .answerIndex(answerIndex)                // 0~3
                    .questionNo(i + 1)                       // 1..N
                    .meaning(choices.get(answerIndex))       // 정답 의미
                    .build());
        }

        // 2) DTO -> 엔티티 변환 후 배치 insert
        List<QuizWord> batch = new ArrayList<>(creates.size());
        for (QuizWordCreate c : creates) {
            batch.add(QuizWord.builder()
                    .quizResult(original)                    // 영속 부모 참조 → 추가 SELECT 없음
                    .questionNo(c.getQuestionNo())
                    .word(c.getWord())
                    .meaning(c.getMeaning())
                    .choice1(c.getChoices().get(0))
                    .choice2(c.getChoices().get(1))
                    .choice3(c.getChoices().get(2))
                    .choice4(c.getChoices().get(3))
                    .correctAnswer(c.getAnswerIndex() + 1)   // 0~3 → 1~4 저장
                    .userAnswer(null)
                    .isCorrect(null)
                    .createdAt(Instant.now())
                    .build());
        }

        quizWordRepository.saveAll(batch);
    }
}