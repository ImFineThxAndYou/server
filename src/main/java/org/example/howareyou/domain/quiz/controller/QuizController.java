package org.example.howareyou.domain.quiz.controller;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.quiz.dto.ClientStartResponse;
import org.example.howareyou.domain.quiz.dto.request.DailyQuizRequest;
import org.example.howareyou.domain.quiz.dto.request.RandomQuizRequest;
import org.example.howareyou.domain.quiz.dto.response.PageResponse;
import org.example.howareyou.domain.quiz.dto.response.QuizResultResponse;
import org.example.howareyou.domain.quiz.dto.submit.SubmitQuizRequest;
import org.example.howareyou.domain.quiz.dto.submit.SubmitResponse;
import org.example.howareyou.domain.quiz.service.QuizGeneratorService;
import org.example.howareyou.domain.quiz.service.QuizService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.example.howareyou.global.security.CustomMemberDetails;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizGeneratorService quizGeneratorService;
    private final QuizService quizService;
    private final MemberService memberService;

    @Operation(
            summary = "단어장 전체범위 퀴즈 생성",
            description = """
            로그인 사용자의 전체 단어장에서 5~30문항을 자동으로 생성합니다.
            - 보기 언어(meaning language)는 기본적으로 회원 프로필의 language를 사용합니다.
            - 필요 시 요청 바디에 `language`(ko|en)를 보내면 프로필 값을 덮어쓸 수 있습니다.
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RandomQuizRequest.class),
                            examples = {
                                    @ExampleObject(name="기본(프로필 언어 사용)", value="""
                        {
                        }
                    """),
                                    @ExampleObject(name="언어 덮어쓰기(예: 영어보기)", value="""
                        {
                          "language": "en"
                        }
                    """)
                            }
                    )
            )
    )
    @PostMapping("/random/start")
    public ResponseEntity<ClientStartResponse> startRandom(
            @AuthenticationPrincipal CustomMemberDetails member,
            @RequestBody @Valid RandomQuizRequest req
    ) {
        if (member == null) throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);

        String memberName = member.getMembername();
        String meaningLang = firstNonBlank(
                req.getLanguage(),
                Optional.ofNullable(memberService.getMemberByMembername(memberName))
                        .map(m -> m.getProfile())
                        .map(p -> p.getLanguage())
                        .orElse("ko")
        );
        ClientStartResponse res =
                quizGeneratorService.startRandomQuiz(memberName, meaningLang, req.getQuizLevel());

        return ResponseEntity.ok(res);
    }

    @Operation(
            summary = "데일리 퀴즈 시작",
            description = """
            로그인 사용자의 특정 날짜 단어장에서 5~30문항을 자동 생성합니다.
            - 요청 바디의 `date`는 yyyy-dd-MM(연-일-월) 문자열입니다. 예) "2025-11-08" (= 2025년 08월 11일)
            - 보기 언어는 프로필 language를 기본으로, `language`로 덮어쓸 수 있습니다.
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = DailyQuizRequest.class),
                            examples = {
                                    @ExampleObject(name="기본(프로필 언어 사용)", value="""
                        {
                          "date": "2025-11-08"
                        }
                    """),
                                    @ExampleObject(name="언어 덮어쓰기(예: 영어보기)", value="""
                        {
                          "date": "2025-11-08",
                          "language": "en"
                        }
                    """)
                            }
                    )
            )
    )
    @PostMapping("/daily/start")
    public ResponseEntity<ClientStartResponse> startDaily(
            @AuthenticationPrincipal CustomMemberDetails member,
            @RequestParam("date") String dateStr,  // yyyy-MM-dd 형식 문자열
            @RequestBody @Valid DailyQuizRequest req
    ) {
        if (member == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // ✅ yyyy-MM-dd 문자열을 LocalDate로 변환
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException ex) {
            throw new CustomException(ErrorCode.INVALID_DATE_FORMAT);
        }

        String memberName = member.getMembername();

        String meaningLang = firstNonBlank(
                req.getLanguage(),
                Optional.ofNullable(memberService.getMemberByMembername(memberName))
                        .map(m -> m.getProfile())
                        .map(p -> p.getLanguage())
                        .orElse("ko")
        );

        ClientStartResponse res =
                quizGeneratorService.startDailyQuiz(memberName, date, meaningLang, req.getQuizLevel());

        return ResponseEntity.ok(res);
    }

    /* ===== 기존 메서드들은 그대로 ===== */

    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    /** yyyy-dd-MM 형태(연-일-월) 전용 파서 */
    private static LocalDate parseDateYDM(String s) {
        try {
            return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-dd-MM"));
        } catch (DateTimeParseException ex) {
            // 프로젝트 공통 에러코드 사용
            throw new CustomException(ErrorCode.INVALID_DATE_FORMAT);
        }
    }
}