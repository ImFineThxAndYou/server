package org.example.howareyou.domain.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.dashboard.dto.DashboardSummary;
import org.example.howareyou.domain.dashboard.dto.WrongAnswer;
import org.example.howareyou.domain.dashboard.service.DashboardService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.example.howareyou.global.security.CustomMemberDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(
        summary = "대시보드 요약 정보 조회",
        description = "사용자의 학습 현황을 종합적으로 조회합니다. 총 단어 개수, 연속 학습일, 복습 필요 날짜, 학습 성과 분석, 오답노트를 포함합니다."
    )
    public ResponseEntity<DashboardSummary> getDashboard(
        @AuthenticationPrincipal CustomMemberDetails member,
        @Parameter(description = "타임존 (기본값: Asia/Seoul)", example = "Asia/Seoul")
        @RequestParam(defaultValue = "Asia/Seoul") String timezone,
        @Parameter(description = "조회 기간 (week/month, 기본값: week)", example = "week")
        @RequestParam(defaultValue = "week") String period
    ) {
        log.info("대시보드 조회 요청 - memberId: {}, timezone: {}, period: {}", member.getId(), timezone, period);
        
        ZoneId zoneId = ZoneId.of(timezone);
        DashboardSummary summary = dashboardService.getDashboardSummary(member.getId(), zoneId, period);
        
        log.info("대시보드 조회 완료 - memberId: {}", member.getId());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/words/count")
    @Operation(
        summary = "단어 개수 조회",
        description = "사용자의 단어 개수를 조회합니다. 언어, 품사별 필터링이 가능합니다."
    )
    public ResponseEntity<Long> countWords(
        @AuthenticationPrincipal CustomMemberDetails member,
        @Parameter(description = "언어 필터 (예: en, ko)", example = "en")
        @RequestParam(required = false) String lang,
        @Parameter(description = "품사 필터 (예: noun, verb)", example = "noun")
        @RequestParam(required = false) String pos,
        @Parameter(description = "조회 기간 (week/month, 기본값: week)", example = "week")
        @RequestParam(defaultValue = "week") String period
    ) {
        log.info("단어 개수 조회 요청 - memberId: {}, lang: {}, pos: {}, period: {}", member.getId(), lang, pos, period);
        
        long count = dashboardService.countWords(member.getId(), lang, pos, period);
        
        log.info("단어 개수 조회 완료 - memberId: {}, count: {}", member.getId(), count);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/learning-streak")
    @Operation(
        summary = "연속 학습일 조회",
        description = "사용자의 연속 학습일을 조회합니다."
    )
    public ResponseEntity<Integer> getLearningStreak(
        @AuthenticationPrincipal CustomMemberDetails member,
        @Parameter(description = "타임존 (기본값: Asia/Seoul)", example = "Asia/Seoul")
        @RequestParam(defaultValue = "Asia/Seoul") String timezone,
        @Parameter(description = "조회 기간 (week/month, 기본값: week)", example = "week")
        @RequestParam(defaultValue = "week") String period
    ) {
        log.info("연속 학습일 조회 요청 - memberId: {}, timezone: {}, period: {}", member.getId(), timezone, period);
        
        ZoneId zoneId = ZoneId.of(timezone);
        int streak = dashboardService.getLearningDays(member.getId(), zoneId, period);
        
        log.info("연속 학습일 조회 완료 - memberId: {}, streak: {}", member.getId(), streak);
        return ResponseEntity.ok(streak);
    }

    @GetMapping("/learning-grass")
    @Operation(
        summary = "학습 잔디 데이터 조회",
        description = "GitHub 스타일의 학습 잔디 데이터를 조회합니다."
    )
    public ResponseEntity<Map<String, Integer>> getLearningGrass(
        @AuthenticationPrincipal CustomMemberDetails member,
        @Parameter(description = "조회 연도", example = "2024")
        @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") int year,
        @Parameter(description = "타임존 (기본값: Asia/Seoul)", example = "Asia/Seoul")
        @RequestParam(defaultValue = "Asia/Seoul") String timezone,
        @Parameter(description = "조회 기간 (week/month, 기본값: week)", example = "week")
        @RequestParam(defaultValue = "week") String period
    ) {
        log.info("학습 잔디 조회 요청 - memberId: {}, year: {}, timezone: {}, period: {}", member.getId(), year, timezone, period);
        
        ZoneId zoneId = ZoneId.of(timezone);
        Map<String, Integer> grass = dashboardService.getLearningGrass(member.getId(), year, zoneId, period);
        
        log.info("학습 잔디 조회 완료 - memberId: {}", member.getId());
        return ResponseEntity.ok(grass);
    }

    @GetMapping("/vocabulary-grass")
    @Operation(
        summary = "단어장 잔디 데이터 조회",
        description = "단어장 학습 잔디 데이터를 조회합니다."
    )
    public ResponseEntity<Map<String, Integer>> getVocabularyGrass(
        @AuthenticationPrincipal CustomMemberDetails member,
        @Parameter(description = "조회 연도", example = "2024")
        @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") int year,
        @Parameter(description = "타임존 (기본값: Asia/Seoul)", example = "Asia/Seoul")
        @RequestParam(defaultValue = "Asia/Seoul") String timezone,
        @Parameter(description = "조회 기간 (week/month, 기본값: week)", example = "week")
        @RequestParam(defaultValue = "week") String period
    ) {
        log.info("단어장 잔디 조회 요청 - memberId: {}, year: {}, timezone: {}, period: {}", member.getId(), year, timezone, period);
        
        ZoneId zoneId = ZoneId.of(timezone);
        Map<String, Integer> grass = dashboardService.getVocabularyGrass(member.getId(), year, zoneId, period);
        
        log.info("단어장 잔디 조회 완료 - memberId: {}", member.getId());
        return ResponseEntity.ok(grass);
    }

    @GetMapping("/quiz-grass")
    @Operation(
        summary = "퀴즈 잔디 데이터 조회",
        description = "퀴즈 학습 잔디 데이터를 조회합니다."
    )
    public ResponseEntity<Map<String, Integer>> getQuizGrass(
        @AuthenticationPrincipal CustomMemberDetails member,
        @Parameter(description = "조회 연도", example = "2024")
        @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") int year,
        @Parameter(description = "타임존 (기본값: Asia/Seoul)", example = "Asia/Seoul")
        @RequestParam(defaultValue = "Asia/Seoul") String timezone,
        @Parameter(description = "조회 기간 (week/month, 기본값: week)", example = "week")
        @RequestParam(defaultValue = "week") String period
    ) {
        log.info("퀴즈 잔디 조회 요청 - memberId: {}, year: {}, timezone: {}, period: {}", member.getId(), year, timezone, period);
        
        ZoneId zoneId = ZoneId.of(timezone);
        Map<String, Integer> grass = dashboardService.getQuizGrass(member.getId(), year, zoneId, period);
        
        log.info("퀴즈 잔디 조회 완료 - memberId: {}", member.getId());
        return ResponseEntity.ok(grass);
    }

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
    @GetMapping("/wrong-answers")
    public ResponseEntity<List<WrongAnswer>> getWrongAnswers(
        @AuthenticationPrincipal CustomMemberDetails member
    ) {
        if (member == null) throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        Long memberId = member.getId();

        log.info("오답노트 조회 요청 - memberId: {}", memberId);
        List<WrongAnswer> response = dashboardService.getWrongAnswer(memberId);
        log.info("오답노트 조회 완료 - memberId: {}, count: {}", memberId, response.size());
        
        return ResponseEntity.ok(response);
    }
}
