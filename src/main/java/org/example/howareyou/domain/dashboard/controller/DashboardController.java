package org.example.howareyou.domain.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.dashboard.dto.DashboardSummary;
import org.example.howareyou.domain.dashboard.dto.ReviewNeededResponse;
import org.example.howareyou.domain.dashboard.dto.ScorePoint;
import org.example.howareyou.domain.dashboard.service.DashboardService;
import org.example.howareyou.global.security.CustomMemberDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.List;
/**
 * 시계열(Time Series) = 시간이 흐름에 따라 기록된 데이터들의 집합
 * TODO : timezone 어디서 가져와요? */

@RestController
@RequiredArgsConstructor
@Tag(name = "마이페이지 대시보드", description = "마이페이지 대시보드 관련 API")
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 1) 총 단어 개수
     */
    @Operation(
            summary = "총 단어 개수",
            description = "사용자 단어장의 총 단어 개수를 반환합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "성공",
                            content = @Content(schema = @Schema(implementation = Long.class)))
            }
    )
    @GetMapping("/total-words")
    public long getTotalWords(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomMemberDetails member
    ) {
        return dashboardService.countWords(member.getMembername());
    }

    /**
     * 2) 연속 학습일 (오늘 기준 연속)
     */
    @Operation(
            summary = "연속 학습일(오늘 기준)",
            description = "quiz_result에서 SUBMIT된 날짜를 기준으로, 오늘부터 끊기지 않은 연속 학습일 수를 계산합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "성공",
                            content = @Content(schema = @Schema(implementation = Integer.class)))
            }
    )
    @GetMapping("/streak")
    public int getLearningStreak(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomMemberDetails member,
            @Parameter(description = "타임존(IANA ID). 기본값: Asia/Seoul", example = "Asia/Seoul")
            @RequestParam(defaultValue = "Asia/Seoul") String zone
    ) {
        return dashboardService.getLearningDays(member.getId(), ZoneId.of(zone));
    }

    /**
     * 3) 복습 필요 날짜 수
     * 정의: “단어장은 생성됐지만, 그 날 퀴즈가 생성되지 않은 날짜 수”
     */
    @Operation(
            summary = "복습 필요 날짜 수",
            description = "지정 기간(기본 최근 30일) 동안 ‘단어장은 생성됐지만, 같은 날 퀴즈는 생성되지 않은’ 날짜 수를 반환합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "성공",
                            content = @Content(schema = @Schema(implementation = ReviewNeededResponse.class)))
            }
    )
    @GetMapping("/review-need")
    public ReviewNeededResponse getReviewNeeded(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomMemberDetails member,
            @Parameter(description = "타임존(IANA ID). 기본값: Asia/Seoul", example = "Asia/Seoul")
            @RequestParam(defaultValue = "Asia/Seoul") String zone
    ) {
        final String membername = member.getMembername();
        final ZoneId zoneId = ZoneId.of(zone);

        // 오늘 포함 최근 30일 (예: 오늘이 8/18이면 7/20 ~ 8/18)
        final LocalDate today = LocalDate.now(zoneId);
        final LocalDate start = today.minusDays(29);
        final LocalDate end   = today; // inclusive

        final int reviewDays = dashboardService.countReviewDays(membername, start, end, zoneId);
        final String message = (reviewDays == 0)
                ? "학습을 꾸준히 하고 계시네요! 최고예요 🎉"
                : null;

        return new ReviewNeededResponse(reviewDays, message, start, end, zone);
    }

    /**
     * 4) 퀴즈 정답률 (최근 30일)
     * 응답의 submittedAtUtc는 UTC 기준
     */
    @Operation(
            summary = "퀴즈 정답률 시계열",
            description = "기본 최근 30일 범위로 quiz_result의 점수(0~100)를 시간 순서대로 반환합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "성공",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ScorePoint.class))))
            }
    )
    @GetMapping("/score-series")
    public List<ScorePoint> getScoreSeries(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomMemberDetails member,
            @Parameter(description = "최대 포인트 개수 제한", example = "1000")
            @RequestParam(required = false, defaultValue = "1000") Integer limit,
            @Parameter(description = "타임존(IANA ID). 기본값: Asia/Seoul", example = "Asia/Seoul")
            @RequestParam(required = false, defaultValue = "Asia/Seoul") String zone
    ) {
        ZoneId zoneId = ZoneId.of(zone);

        LocalDate endLocal = LocalDate.now(zoneId);
        LocalDate startLocal = endLocal.minusDays(29);
        Instant fromUtc = startLocal.atStartOfDay(zoneId).toInstant();
        Instant toUtc = endLocal.plusDays(1).atStartOfDay(zoneId).toInstant();

        return dashboardService.getQuizScoreSeries(member.getId(), fromUtc, toUtc, limit);
    }

    /**
     * 0) 대시보드 요약 한 번에 받기
     * - 총 단어 수, 오늘 기준 연속 학습일, 복습 필요 날짜 수(최근 30일), 점수 시계열(최근 30일)
     * - 위에 작성된 api 한번에 받기. 필요한경우 사용해주세요 (네트워크 절갑, 데이터 동기화의 장점있음)
     */
    @Operation(
            summary = "대시보드 요약",
            description = "전체 지표를 한 번에 반환합니다. (총 단어 수, 연속 학습일, 복습 필요 일수, 점수 시계열)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "성공",
                            content = @Content(schema = @Schema(implementation = DashboardSummary.class)))
            }
    )
    @GetMapping("/summary")
    public DashboardSummary getSummary(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomMemberDetails member,
            @Parameter(description = "타임존(IANA ID). 기본값: Asia/Seoul", example = "Asia/Seoul")
            @RequestParam(defaultValue = "Asia/Seoul") String zone,
            @Parameter(description = "점수 시계열 최대 포인트 개수", example = "1000")
            @RequestParam(required = false, defaultValue = "1000") Integer limit
    ) {
        String membername = member.getMembername();
        Long memberId = member.getId();
        ZoneId zoneId = ZoneId.of(zone);

        // 최근 30일 로컬 기간
        LocalDate today = LocalDate.now(zoneId);
        LocalDate start = today.minusDays(29);
        LocalDate end   = today;

        // 총 단어
        long totalWords = dashboardService.countWords(membername);

        // 연속 학습일 (오늘 기준)
        int streak = dashboardService.getLearningDays(memberId, zoneId);

        // 복습 필요일수
        int reviewDays = dashboardService.countReviewDays(membername, start, end, zoneId);
        String encouragement = (reviewDays == 0) ? "학습을 꾸준히 하시고계시는군요? 최고에요" : null; // 문구마음에안들면 갈아치우기 가능

        // 점수 시계열(최근 30일) — UTC 경계로 변환
        Instant toUtc = end.plusDays(1).atStartOfDay(zoneId).toInstant();
        Instant fromUtc = start.atStartOfDay(zoneId).toInstant();
        List<ScorePoint> series = dashboardService.getQuizScoreSeries(memberId, fromUtc, toUtc, limit);

        return new DashboardSummary(totalWords, streak, reviewDays, encouragement, series);
    }
}