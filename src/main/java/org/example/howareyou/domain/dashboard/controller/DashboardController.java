package org.example.howareyou.domain.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.dashboard.dto.WrongAnswer;
import org.example.howareyou.domain.dashboard.service.DashboardService;
import org.example.howareyou.domain.quiz.dto.response.WrongAnswerResponse;
import org.example.howareyou.domain.quiz.service.QuizServiceImpl;
import org.example.howareyou.domain.vocabulary.service.ChatVocaBookService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.example.howareyou.global.security.CustomMemberDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 오답노트
     */
    @Operation(
            summary = "오답노트",
            description = "사용자가 가장 최근에 푼 퀴즈에서 틀린 단어의 word-meaning-pos 가져와서 제공"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "오답노트 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 오답노트가 존재하지 않음")
    })
    @GetMapping("/{membername}/wrong-answers")
    public ResponseEntity<List<WrongAnswer>> getVocabularyListByChatRoom(
            @AuthenticationPrincipal CustomMemberDetails member
    ) {
        if (member == null) throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        Long memberId = member.getId();

        List<WrongAnswer> response = dashboardService.getWrongAnswer(memberId);
        return ResponseEntity.ok(response);
    }
}
