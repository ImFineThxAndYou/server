package org.example.howareyou.domain.quiz.service;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.quiz.dto.ClientQuizQuestion;
import org.example.howareyou.domain.quiz.dto.ClientStartResponse;
import org.example.howareyou.domain.quiz.dto.response.QuizResultResponse;
import org.example.howareyou.domain.quiz.dto.submit.SubmitResponse;
import org.example.howareyou.domain.quiz.entity.QuizResult;
import org.example.howareyou.domain.quiz.entity.QuizStatus;
import org.example.howareyou.domain.quiz.entity.QuizWord;
import org.example.howareyou.domain.quiz.repository.QuizResultRepository;
import org.example.howareyou.domain.quiz.repository.QuizWordRepository;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizServiceImpl implements QuizService {

    private final QuizResultRepository quizResultRepository;
    private final QuizWordRepository quizWordRepository;

    /**
     * 퀴즈 채점 및 상태 업데이트
     * @param quizUuid   퀴즈 고유 식별자(UUID)
     * @param selected   사용자가 제출한 각 문항의 선택 번호 리스트 (1~4, 미선택은 -1)
     * @return 채점 결과(정답 개수, 총 문항 수, 점수 등)
     */
    @Override
    public SubmitResponse gradeQuiz(String quizUuid, List<Integer> selected) {
        // TODO : 제출하는사람의 member id 가 동일한지 확인하는 로직추가 (방어로직)
        // 1) 제출 여부 확인 (uuid 기반)
        boolean completed = quizResultRepository.findCompletedQuizByUuid(quizUuid)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_NOT_FOUND));
        if (completed) throw new CustomException(ErrorCode.QUIZ_ALREADY_SUBMITTED);

        // 2) 내부 PK(Long) 조회 (한 번만)
        Long quizResultId = quizResultRepository.findByUuid(quizUuid)
                .map(QuizResult::getId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_NOT_FOUND));

        // 3) 채점용 문항 경량 조회 (id 기반)
        var items = quizWordRepository.findForGrading(quizResultId);

        if (items.size() != selected.size()) {
            throw new CustomException(ErrorCode.INVALID_SELECTION_COUNT);
        }

        int correct = 0;
        /* 채점 */
        for (int i = 0; i < items.size(); i++) {
            var view = items.get(i);
            int choiceSize = view.choiceSize();
            int selRaw = selected.get(i); // 클라이언트 값: Null 또는 1..4

            // 1-based 그대로 저장할 값
            Integer userAnswerToStore;
            if (selRaw == -1) {
                userAnswerToStore = null; // 미선택은 null
            } else if (selRaw >= 1 && selRaw <= choiceSize) {
                userAnswerToStore = selRaw; // 1..4 유지
            } else {
                throw new CustomException(ErrorCode.INVALID_SELECTION_INDEX);
            }

            // 1-based 끼리 비교
            boolean isCorrect = (userAnswerToStore != null)
                    && userAnswerToStore.equals(view.getCorrectAnswer());

            // 저장도 1-based 그대로
            quizWordRepository.applyGrading(view.getId(), userAnswerToStore, isCorrect);
            if (isCorrect) correct++;
        }

        long total = items.size();
        long score = Math.round(correct * 100.0 / total);

        // 4) 결과 집계 업데이트 (uuid 기반)
        quizResultRepository.finalizeGradingByUuid(quizUuid, correct, total, score, Instant.now(), QuizStatus.SUBMIT);

        // 5) 응답
        return SubmitResponse.builder()
                .quizUUID(quizUuid)
                .correctCount(correct)
                .totalQuestions((int) total)
                .score(score)
                .build();
    }
    /* 멤버벌 퀴즈 조회 (전체)*/
    @Override
    public Page<QuizResultResponse> getQuizResultsByMember(Long memberId,QuizStatus status, Pageable pageable) {
        Page<QuizResult> page = (status == null)
                ? quizResultRepository.findByMemberId(memberId, pageable)
                : quizResultRepository.findByMemberIdAndQuizStatus(memberId, status, pageable);
        return quizResultRepository.findByMemberIdAndOptionalStatus(memberId,status, pageable)
                .map(QuizResultResponse::fromEntity);
    }
    /* 단건조회 */
    @Override
    public QuizResultResponse getQuizResultDetail(String quizUUID) {
        QuizResult result = quizResultRepository.findByUuid(quizUUID)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_NOT_FOUND));

        return QuizResultResponse.fromEntity(result);
    }

    @Override
    public ClientStartResponse getPendingQuizByUuid(Long memberId, String uuid) {
        QuizResult qr = quizResultRepository.findByUuid(uuid)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_NOT_FOUND));

        if (!Objects.equals(qr.getMemberId(), memberId)) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        if (qr.getQuizStatus() != QuizStatus.PENDING) {
            throw new CustomException(ErrorCode.QUIZ_ALREADY_SUBMITTED); // 제출 완료는 재개 불가
        }

        List<QuizWord> words = quizWordRepository.findByQuizResultIdOrderByQuestionNo(qr.getId());

        List<ClientQuizQuestion> questions = new ArrayList<>(words.size());
        for (QuizWord w : words) {
            List<String> choices = List.of(
                    w.getChoice1(), w.getChoice2(), w.getChoice3(), w.getChoice4()
            );
            questions.add(ClientQuizQuestion.builder()
                    .questionNo(w.getQuestionNo())
                    .question(w.getWord())
                    .choices(choices)
                    .build());
        }

        return ClientStartResponse.builder()
                .quizResultId(qr.getId())
                .quizUUID(qr.getUuid())
                .quizQuestions(questions)
                .build();
    }
}