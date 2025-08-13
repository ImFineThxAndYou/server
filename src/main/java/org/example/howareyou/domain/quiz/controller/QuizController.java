package org.example.howareyou.domain.quiz.controller;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.quiz.dto.ClientStartResponse;
import org.example.howareyou.domain.quiz.dto.request.DailyQuizRequest;
import org.example.howareyou.domain.quiz.dto.request.RandomQuizRequest;
import org.example.howareyou.domain.quiz.dto.submit.SubmitQuizRequest;
import org.example.howareyou.domain.quiz.dto.submit.SubmitResponse;
import org.example.howareyou.domain.quiz.service.QuizGeneratorService;
import org.example.howareyou.domain.quiz.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizGeneratorService quizGeneratorService;
    private final QuizService quizService;
    private final MemberService memberService;

    @Operation(
            summary = "범위: 단어장 전체 퀴즈 시작",
            description = """
            회원의 전체 단어장에서 5~30문항을 자동으로 생성합니다.
            - 보기 언어(meaning language)는 기본적으로 회원 프로필의 language를 사용합니다.
            - 필요 시 요청 바디에 `language`(ko|en)를 보내면 프로필 값을 덮어쓸 수 있습니다.
            - 문항 수는 서버가 단어장 상황에 맞춰 자동 결정합니다(클라이언트에서 보내지 않음).
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RandomQuizRequest.class),
                            examples = {
                                    @ExampleObject(name="기본(프로필 언어 사용)", value="""
                        {
                          "memberName": "user1"
                        }
                    """),
                                    @ExampleObject(name="언어 덮어쓰기(예: 영어보기)", value="""
                        {
                          "memberName": "user1",
                          "language": "en"
                        }
                    """)
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공", content = @Content(schema = @Schema(implementation = ClientStartResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/random/start")
    public ResponseEntity<ClientStartResponse> startRandom(@RequestBody @Valid RandomQuizRequest req) {
        String memberName = req.getMemberName();

        String meaningLang = firstNonBlank(
                req.getLanguage(),
                (req.getMemberProfile() != null) ? req.getMemberProfile().getLanguage() : null,
                // 마지막fallback: DB 프로필 언어, 없으면 "ko"
                Optional.ofNullable(memberService.getMemberByMembername(memberName))
                        .map(m -> m.getProfile())
                        .map(p -> p.getLanguage())
                        .orElse("ko")
        );

        ClientStartResponse res = quizGeneratorService.startRandomQuiz(memberName, meaningLang);
        return ResponseEntity.ok(res);
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    @Operation(
            summary = "데일리 퀴즈 시작",
            description = """
            특정 날짜의 단어장에서 5~30문항을 자동으로 생성합니다.
            - 보기 언어(meaning language)는 기본적으로 회원 프로필의 language를 사용합니다.
            - 필요 시 요청 바디에 `language`(ko|en)를 보내면 프로필 값을 덮어쓸 수 있습니다.
            - 문항 수는 서버가 해당 날짜의 단어 수를 보고 자동 결정합니다(클라이언트에서 보내지 않음).
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = DailyQuizRequest.class),
                            examples = {
                                    @ExampleObject(name="기본(프로필 언어 사용)", value="""
                        {
                          "membername": "user1",
                          "date": "2025-08-11"
                        }
                    """),
                                    @ExampleObject(name="언어 덮어쓰기(예: 영어보기)", value="""
                        {
                          "membername": "user1",
                          "date": "2025-08-11",
                          "language": "en"
                        }
                    """)
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공", content = @Content(schema = @Schema(implementation = ClientStartResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/daily/start")
    public ResponseEntity<ClientStartResponse> startDaily(@RequestBody @Valid DailyQuizRequest req) {// ⚠️ Swagger 예시의 "membername"과 필드명이 다르면 바인딩 안 됨
        LocalDate date    = req.getDate();
        String memberName = req.getMemberName();

        String meaningLang = firstNonBlank(
                req.getLanguage(),
                (req.getMemberProfile() != null) ? req.getMemberProfile().getLanguage() : null,
                // 마지막fallback: DB 프로필 언어, 없으면 "ko"
                Optional.ofNullable(memberService.getMemberByMembername(memberName))
                        .map(m -> m.getProfile())
                        .map(p -> p.getLanguage())
                        .orElse("ko")
        );

        ClientStartResponse res = quizGeneratorService.startDailyQuiz(memberName, date, meaningLang);
        return ResponseEntity.ok(res);
    }

    @Operation(
            summary = "퀴즈 제출(채점)",
            description = "퀴즈의 각 문항 선택 인덱스 배열(0~3, 미선택은 -1)을 제출하면 서버에서 채점합니다.",
            parameters = {
                    @Parameter(name = "quizUUID", description = "퀴즈 공개용 UUID", example = "3b8b5e6a-1e24-4d2f-9d2a-7a1b0f2ec1aa")
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = SubmitQuizRequest.class),
                            examples = @ExampleObject(name="기본", value="""
                        {
                          "selectedIndexes": [
                            2, 2, 2, 2, 2,
                            2, 2, 2, 2, 2,
                            2, 2, 2, 2, 2,
                            2, 2, 2, 2, 2,
                            2, 2, 2, 2, 2,
                            2, 2, 2, 2, 2
                          ]
                        }
                """)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채점 성공", content = @Content(schema = @Schema(implementation = SubmitResponse.class))),
            @ApiResponse(responseCode = "400", description = "검증 실패"),
            @ApiResponse(responseCode = "404", description = "퀴즈 없음"),
            @ApiResponse(responseCode = "409", description = "이미 제출됨")
    })
    @PostMapping("/{quizUUID}/submit")
    public ResponseEntity<SubmitResponse> submit(
            @PathVariable String quizUUID,
            @Valid @RequestBody SubmitQuizRequest req
    ) {
        SubmitResponse res = quizService.gradeQuiz(quizUUID, req.getSelectedIndexes());
        return ResponseEntity.ok(res);
    }
}