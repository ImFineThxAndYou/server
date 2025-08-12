package org.example.howareyou.domain.quiz.service;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.quiz.dto.submit.SubmitResponse;
import org.example.howareyou.domain.quiz.entity.QuizResult;
import org.example.howareyou.domain.quiz.repository.QuizResultRepository;
import org.example.howareyou.domain.quiz.repository.QuizWordRepository;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizServiceImpl implements QuizService {

    private final QuizResultRepository quizResultRepo;
    private final QuizWordRepository quizWordRepo;

    @Override
    public SubmitResponse gradeQuiz(String quizUuid, List<Integer> selected) {
        // 1) 제출 여부 확인 (uuid 기반)
        boolean completed = quizResultRepo.findCompletedQuizByUuid(quizUuid)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_NOT_FOUND));
        if (completed) throw new CustomException(ErrorCode.QUIZ_ALREADY_SUBMITTED);

        // 2) 내부 PK(Long) 조회 (한 번만)
        Long quizResultId = quizResultRepo.findByUuid(quizUuid)
                .map(QuizResult::getId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_NOT_FOUND));

        // 3) 채점용 문항 경량 조회 (id 기반)
        var items = quizWordRepo.findForGrading(quizResultId);

        if (items.size() != selected.size()) {
            throw new CustomException(ErrorCode.INVALID_SELECTION_COUNT);
        }

        int correct = 0;
        for (int i = 0; i < items.size(); i++) {
            var view = items.get(i);
            int sel = selected.get(i);

            int choiceSize = view.choiceSize(); // 보기 개수 계산
            if (sel < -1 || sel >= choiceSize) {
                throw new CustomException(ErrorCode.INVALID_SELECTION_INDEX);
            }

            // 클라: 0~3 / DB 정답: 1~4
            boolean isCorrect = (sel >= 0) && (sel + 1 == view.getCorrectAnswer());
            quizWordRepo.applyGrading(view.getId(), sel, isCorrect);

            if (isCorrect) correct++;
        }

        long total = items.size();
        long score = Math.round(correct * 100.0 / total);

        // 4) 결과 집계 업데이트 (uuid 기반)
        quizResultRepo.finalizeGradingByUuid(quizUuid, correct, total, score, Instant.now());

        // 5) 응답
        return SubmitResponse.builder()
                .quizUUID(quizUuid)               // 내부 PK는 노출 X, 공개용 uuid만
                .correctCount(correct)
                .totalQuestions((int) total)
                .score(score)
                .build();
    }
}