package org.example.howareyou.domain.quiz.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.quiz.dto.ClientStartResponse;
import org.example.howareyou.domain.quiz.dto.request.DailyQuizRequest;
import org.example.howareyou.domain.quiz.dto.request.RandomQuizRequest;
import org.example.howareyou.domain.quiz.dto.submit.SubmitQuizRequest;
import org.example.howareyou.domain.quiz.dto.submit.SubmitResponse;
import org.example.howareyou.domain.quiz.service.QuizGeneratorService;
import org.example.howareyou.domain.quiz.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizGeneratorService quizGeneratorService;
    private final QuizService quizService;

    /** 전체 단어장에서 5~30문항 생성 */
    @PostMapping("/random/start")
    public ResponseEntity<ClientStartResponse> startRandom(@RequestBody @Valid RandomQuizRequest req) {
        return ResponseEntity.ok(
                quizGeneratorService.startRandomQuiz(
                        req.getMemberId(),
                        req.getMeaningLang(),
                        req.getCount()
                )
        );
    }

    /** 특정 날짜 단어장에서 5~30문항 생성 (부족하면 전체에서 오답 보강) */
    @PostMapping("/daily/start")
    public ResponseEntity<ClientStartResponse> startDaily(@RequestBody @Valid DailyQuizRequest req) {
        return ResponseEntity.ok(
                quizGeneratorService.startDailyQuiz(
                        req.getMemberId(),
                        req.getDate(),
                        req.getMeaningLang(),
                        req.getCount()
                )
        );
    }
    /** 퀴즈 정답 */
    @PostMapping("/{quizUUID}/submit")
    public ResponseEntity<SubmitResponse> submit(
            @PathVariable String quizUUID,
            @Valid @RequestBody SubmitQuizRequest req
    ) {
        SubmitResponse res = quizService.gradeQuiz(quizUUID, req.getSelectedIndexes());
        return ResponseEntity.ok(res);
    }
}