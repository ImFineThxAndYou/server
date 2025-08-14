package org.example.howareyou.domain.quiz.controller;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.quiz.dto.ClientStartResponse;
import org.example.howareyou.domain.quiz.dto.response.QuizResultResponse;
import org.example.howareyou.domain.quiz.dto.submit.SubmitQuizRequest;
import org.example.howareyou.domain.quiz.dto.submit.SubmitResponse;
import org.example.howareyou.domain.quiz.entity.QuizLevel;
import org.example.howareyou.domain.quiz.entity.QuizStatus;
import org.example.howareyou.domain.quiz.service.QuizGeneratorService;
import org.example.howareyou.domain.quiz.service.QuizService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.example.howareyou.global.security.CustomMemberDetails;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        level: A/B/C 또는 미지정(null) → 전체 난이도 랜덤
    """,
            parameters = {
                    @Parameter(name = "level", description = "A/B/C 또는 전체 난이도 랜덤", example = "")
            }
    )
    @PostMapping("/random/start")
    public ResponseEntity<ClientStartResponse> startRandom(
            @AuthenticationPrincipal CustomMemberDetails member,
            @RequestParam(name = "level", required = false) String levelParam // A/B/C 또는 null
    ) {
        if (member == null) throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);

        String memberName = member.getMembername();

        // null -> 전체 랜덤, A/B/C -> 해당 난이도
        QuizLevel level = QuizLevel.fromParam(levelParam);

        ClientStartResponse res =
                quizGeneratorService.startRandomQuiz(memberName, level);
        return ResponseEntity.ok(res);
    }

    @Operation(
            summary = "데일리 퀴즈 시작",
            description = """
            로그인 사용자의 특정 날짜 단어장에서 5~30문항을 자동 생성합니다.
            - 요청 바디의 `date`는 yyyy-dd-MM(연-일-월) 문자열입니다. 예) "2025-11-08" (= 2025년 08월 11일)
            """
    )
    @PostMapping("/daily/start")
    public ResponseEntity<ClientStartResponse> startDaily(
            @AuthenticationPrincipal CustomMemberDetails member,
            @RequestParam("date") String dateStr             // yyyy-MM-dd
//            @RequestParam(value = "quizLevel", required = false) QuizLevel quizLevel
    ) {
        if (member == null) throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException ex) {
            throw new CustomException(ErrorCode.INVALID_DATE_FORMAT);
        }

        String memberName = member.getMembername();

        ClientStartResponse res =
                quizGeneratorService.startDailyQuiz(memberName, date);

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

    @Operation(
            summary = "내 퀴즈 전체 조회(페이징)",
            description = """
            로그인한 사용자의 모든 퀴즈 결과를 최신순으로 반환합니다.
            - page: 0부터 시작
            - size: 페이지 크기 (기본 20)
            - status(optional): PENDING | SUBMIT | NULL(널이면 전체조회) 필터
            - type(optional): RANDOM | DAILY 필터
            """,
            parameters = {
                    @Parameter(name = "page", example = "0"),
                    @Parameter(name = "size", example = "20"),
                    @Parameter(name = "status", example = "SUBMIT") // (PENDING / SUBMIT)
            }
    )
    // org.example.howareyou.domain.quiz.controller.QuizController
    @GetMapping("/me")
    public ResponseEntity<?> getMyQuizzes(
            @AuthenticationPrincipal CustomMemberDetails member,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) QuizStatus status // ★ PENDING | SUBMIT (enum과 일치)
    ) {
        if (member == null) throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);

        Long memberId = memberService.getIdByMembername(member.getMembername());
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.max(size,1), Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(quizService.getQuizResultsByMember(memberId, status, pageable));
    }

    @Operation(
            summary = "레벨 지정 랜덤 퀴즈 생성(전체범위)",
            description = """
            POST /random/start 와 동일 기능이지만, 쿼리스트링으로 간단 호출 가능한 GET 버전입니다.
            - quizLevel(optional): A1|A2|B1|B2|C1|C2
            - 공백으로 요청하면 /random/start 랑 동일해요 
            """,
            parameters = {
                    @Parameter(name = "quizLevel", example = "A2")
            }
    )
    @PostMapping ("/random/start-level")
    public ResponseEntity<ClientStartResponse> startRandomByLevel(
            @AuthenticationPrincipal CustomMemberDetails member,
            @RequestParam(value = "quizLevel", required = false) QuizLevel quizLevel
    ) {
        if (member == null) throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        String memberName = member.getMembername();



        ClientStartResponse res = quizGeneratorService.startRandomQuiz(memberName, quizLevel);
        return ResponseEntity.ok(res);
    }

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