package org.example.howareyou.global.test;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.quiz.dto.ClientStartResponse;
import org.example.howareyou.domain.quiz.dto.response.QuizResultResponse;
import org.example.howareyou.domain.quiz.dto.submit.SubmitQuizRequest;
import org.example.howareyou.domain.quiz.dto.submit.SubmitResponse;
import org.example.howareyou.domain.quiz.entity.QuizLevel;
import org.example.howareyou.domain.quiz.entity.QuizResult;
import org.example.howareyou.domain.quiz.entity.QuizStatus;
import org.example.howareyou.domain.quiz.repository.QuizResultRepository;
import org.example.howareyou.domain.quiz.service.QuizGeneratorService;
import org.example.howareyou.domain.quiz.service.QuizService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/quiz")
@Tag(name = "테스트", description = "개발 및 테스트용 API (개발 환경에서만 사용)")
public class QuizTestController {

    private final QuizGeneratorService quizGeneratorService;
    private final QuizService quizService;
    private final MemberService memberService;
    private final QuizResultRepository quizResultRepository;

    @Operation(
            summary = "단어장 전체범위 퀴즈 생성(테스트용)",
            description = "membername을 직접 전달하여 퀴즈 생성. level=A/B/C 또는 null"
    )
    @PostMapping("/random/start")
    public ResponseEntity<ClientStartResponse> startRandom(
            @RequestParam("membername") String membername,
            @RequestParam(name = "level", required = false) String levelParam
    ) {
        if (membername == null || membername.isBlank()) throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        QuizLevel level = QuizLevel.fromParam(levelParam);
        ClientStartResponse res = quizGeneratorService.startRandomQuiz(membername, level);
        return ResponseEntity.ok(res);
    }

    @Operation(
            summary = "데일리 퀴즈 시작(테스트용)",
            description = "membername + date(yyyy-MM-dd)로 데일리 퀴즈 생성"
    )
    @PostMapping("/daily/start")
    public ResponseEntity<ClientStartResponse> startDaily(
            @RequestParam("membername") String membername,
            @RequestParam("date") String dateStr
    ) {
        if (membername == null || membername.isBlank()) throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException ex) {
            throw new CustomException(ErrorCode.INVALID_DATE_FORMAT);
        }
        ClientStartResponse res = quizGeneratorService.startDailyQuiz(membername, date);
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "퀴즈 제출(채점, 테스트용)")
    @PostMapping("/{uuid}/submit")
    public SubmitResponse submit(
            @PathVariable String uuid,
            @RequestBody @Valid SubmitQuizRequest req
    ) {
        return quizService.gradeQuiz(uuid, req.selected());
    }

    @Operation(summary = "퀴즈 단건 조회(테스트용)")
    @GetMapping("/{quizUUID}")
    public ResponseEntity<QuizResultResponse> getOne(@PathVariable String quizUUID) {
        QuizResultResponse res = quizService.getQuizResultDetail(quizUUID);
        return ResponseEntity.ok(res);
    }

    @Operation(
            summary = "내 퀴즈 전체 조회(테스트용)",
            description = "membername으로 본인 퀴즈 페이징 조회. status=PENDING|SUBMIT|NULL"
    )
    @GetMapping("/me")
    public ResponseEntity<?> getMyQuizzes(
            @RequestParam("membername") String membername,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) QuizStatus status
    ) {
        if (membername == null || membername.isBlank()) throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        Long memberId = memberService.getIdByMembername(membername);
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.max(size,1), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(quizService.getQuizResultsByMember(memberId, status, pageable));
    }

    @Operation(summary = "레벨 지정 랜덤 퀴즈 생성(테스트용)")
    @PostMapping("/random/level/start")
    public ResponseEntity<ClientStartResponse> startRandomByLevel(
            @RequestParam("membername") String membername,
            @RequestParam(value = "quizLevel", required = false) QuizLevel quizLevel
    ) {
        if (membername == null || membername.isBlank()) throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        ClientStartResponse res = quizGeneratorService.startRandomQuiz(membername, quizLevel);
        return ResponseEntity.ok(res);
    }

    // 부하테스트를 위해 임의추가
    @GetMapping("/uuids")
    public ResponseEntity<?> getUuids(
            @RequestParam String membername,
            @RequestParam(required = false) QuizStatus status,        // SUBMIT | PENDING | null
            @RequestParam(defaultValue = "10000") int limit           // 가져올 최대 개수
    ) {
        Long memberId = memberService.getIdByMembername(membername);

        int size = Math.min(Math.max(limit, 1), 100_000);
        PageRequest pr = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<QuizResult> page = quizResultRepository.findByMemberIdAndStatus(memberId, status, pr);
        var uuids = page.getContent().stream()
                .map(QuizResult::getUuid)
                .toList();

        return ResponseEntity.ok(uuids); // JSON 배열로 반환
    }
}