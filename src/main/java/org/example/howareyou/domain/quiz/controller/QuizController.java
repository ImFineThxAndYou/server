package org.example.howareyou.domain.quiz.controller;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.quiz.dto.ClientStartResponse;
import org.example.howareyou.domain.quiz.dto.request.DailyQuizRequest;
import org.example.howareyou.domain.quiz.dto.request.RandomQuizRequest;
import org.example.howareyou.domain.quiz.dto.response.QuizResultResponse;
import org.example.howareyou.domain.quiz.dto.submit.SubmitQuizRequest;
import org.example.howareyou.domain.quiz.dto.submit.SubmitResponse;
import org.example.howareyou.domain.quiz.entity.QuizLevel;
import org.example.howareyou.domain.quiz.service.QuizGeneratorService;
import org.example.howareyou.domain.quiz.service.QuizService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.example.howareyou.global.security.CustomMemberDetails;
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
            """
    )
    @PostMapping("/daily/start")
    public ResponseEntity<ClientStartResponse> startDaily(
            @AuthenticationPrincipal CustomMemberDetails member,
            @RequestParam("date") String dateStr,              // yyyy-MM-dd
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "quizLevel", required = false) QuizLevel quizLevel
    ) {
        if (member == null) throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException ex) {
            throw new CustomException(ErrorCode.INVALID_DATE_FORMAT);
        }

        String memberName = member.getMembername();
        String meaningLang = firstNonBlank(
                language,
                Optional.ofNullable(memberService.getMemberByMembername(memberName))
                        .map(m -> m.getProfile())
                        .map(p -> p.getLanguage())
                        .orElse("ko")
        );

        ClientStartResponse res =
                quizGeneratorService.startDailyQuiz(memberName, date, meaningLang, quizLevel);

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
    @PostMapping("/{quizUUID}/submit")
    public ResponseEntity<SubmitResponse> submit(
            @PathVariable String quizUUID,
            @Valid @RequestBody SubmitQuizRequest req
    ) {
        SubmitResponse res = quizService.gradeQuiz(quizUUID, req.getSelectedIndexes());
        return ResponseEntity.ok(res);
    }
    /* ==================== 조회/재시작/레벨별 생성 ==================== */

    @Operation(
            summary = "퀴즈 단건 조회",
            description = "퀴즈 UUID로 문항(보기 포함)을 다시 가져옵니다. 이미 시작했던 퀴즈 재로딩 용도.",
            parameters = @Parameter(name = "quizUUID", example = "3b8b5e6a-1e24-4d2f-9d2a-7a1b0f2ec1aa")
    )
    @GetMapping("/{quizUUID}")
    public ResponseEntity<QuizResultResponse> getOne(
            @PathVariable String quizUUID
    ) {
        QuizResultResponse res = quizService.getQuizResultDetail(quizUUID);
        return ResponseEntity.ok(res);
    }

//    @Operation(
//            summary = "내 퀴즈 전체 조회(페이징)",
//            description = """
//            로그인한 사용자의 모든 퀴즈 결과를 최신순으로 반환합니다.
//            - page: 0부터 시작
//            - size: 페이지 크기 (기본 20)
//            - status(optional): COMPLETED | IN_PROGRESS 필터
//            - type(optional): RANDOM | DAILY 필터
//            """,
//            parameters = {
//                    @Parameter(name = "page", example = "0"),
//                    @Parameter(name = "size", example = "20"),
//                    @Parameter(name = "status", example = "IN_PROGRESS"),
//                    @Parameter(name = "type", example = "RANDOM")
//            }
//    )
//    @GetMapping("/me")
//    public ResponseEntity<?> getMyQuizzes(
//            @AuthenticationPrincipal CustomMemberDetails member,
//            @RequestParam(defaultValue = "0") int page
//    ) {
//        if (member == null) throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
//        String memberName = member.getMembername();
//        return ResponseEntity.ok(
//                quizService.getQuizResultsByMember(memberName, page)
//        );
//    }

    @Operation(
            summary = "레벨 지정 랜덤 퀴즈 생성(전체범위)",
            description = """
            POST /random/start 와 동일 기능이지만, 쿼리스트링으로 간단 호출 가능한 GET 버전입니다.
            - language(optional): ko | en (미지정 시 프로필 language)
            - quizLevel(optional): A1|A2|B1|B2|C1|C2
            """,
            parameters = {
                    @Parameter(name = "language", example = "ko"),
                    @Parameter(name = "quizLevel", example = "A2")
            }
    )
    @GetMapping("/random/start-level")
    public ResponseEntity<ClientStartResponse> startRandomByLevel(
            @AuthenticationPrincipal CustomMemberDetails member,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "quizLevel", required = false) QuizLevel quizLevel
    ) {
        if (member == null) throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        String memberName = member.getMembername();

        String meaningLang = firstNonBlank(
                language,
                Optional.ofNullable(memberService.getMemberByMembername(memberName))
                        .map(m -> m.getProfile())
                        .map(p -> p.getLanguage())
                        .orElse("ko")
        );

        ClientStartResponse res = quizGeneratorService.startRandomQuiz(memberName, meaningLang, quizLevel);
        return ResponseEntity.ok(res);
    }

//    @Operation(
//            summary = "미완료(안 푼) 퀴즈 이어 풀기",
//            description = "가장 최근의 진행중(IN_PROGRESS) 퀴즈를 불러옵니다. 없으면 404 응답."
//    )
//    @GetMapping("/unfinished")
//    public ResponseEntity<ClientStartResponse> getUnfinished(
//            @AuthenticationPrincipal CustomMemberDetails member
//    ) {
//        if (member == null) throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
//        String memberName = member.getMembername();
//
//        return quizService.getUnfinishedQuiz(memberName)
//                .map(ResponseEntity::ok)
//                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_NOT_FOUND));
//    }
    /* ===== 내부 유틸 ===== */

    /* 공백 */
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