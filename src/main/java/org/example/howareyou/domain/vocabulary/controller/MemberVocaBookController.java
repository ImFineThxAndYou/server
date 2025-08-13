package org.example.howareyou.domain.vocabulary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.vocabulary.document.MemberVocabulary;
import org.example.howareyou.domain.vocabulary.dto.AggregatedWordEntry;
import org.example.howareyou.domain.vocabulary.service.MemberVocaBookService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/vocabook/member")
@RequiredArgsConstructor
public class MemberVocaBookController {

    private final MemberVocaBookService memberVocaBookService;

    /**
     * 전체 사용자 단어장 목록 조회
     */
    @GetMapping("/all")
    public List<MemberVocabulary> getAllMemberVocabularies() {
        log.info("📖 전체 사용자 단어장 조회 요청");
        return memberVocaBookService.findAll();
    }

    /**
     * 특정 사용자의 전체 단어장 목록 조회
     */
    @Operation(
            summary = "특정 사용자의 전체 단어장 목록 조회",
            description = "사용자(membername)별로 저장된 모든 단어장을 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{membername}")
    public List<MemberVocabulary> getVocabulariesByMember(
            @Parameter(description = "사용자 이름", example = "user1", required = true)
            @PathVariable String membername) {
        log.info("📖 사용자 [{}] 단어장 목록 조회", membername);
        return memberVocaBookService.findByMembername(membername);
    }

    /**
     * 특정 사용자의 날짜별 단어장 조회
     * - document ID: membername_yyyy-MM-dd
     */
    @Operation(
            summary = "특정 사용자의 날짜별 단어장 조회",
            description = "지정한 날짜(yyyy-MM-dd)의 단어장을 반환합니다. "
                    + "저장 시 사용된 날짜는 사용자 타임존 기준입니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "단어장을 찾을 수 없음")
    })
    @GetMapping("/{membername}/{date}")
    public MemberVocabulary getVocabulariesByMemberAndDate(
            @Parameter(description = "사용자 이름", example = "user1", required = true)
            @PathVariable String membername,
            @Parameter(description = "단어장 생성 날짜 (yyyy-MM-dd)", example = "2025-08-12", required = true)
            @PathVariable String date
    ) {
        log.info("📖 사용자 [{}]의 [{}] 날짜 단어장 조회", membername, date);
        LocalDate localDate = LocalDate.parse(date); // yyyy-MM-dd 형식
        return memberVocaBookService.findByMembernameAndDate(membername, localDate);
    }

    /**
     * 특정 사용자의 난이도별 단어장 조회
     */
    @Operation(
            summary = "난이도별 단어 조회(사용자 전체, 레포지토리 집계)",
            description = "레포지토리 @Aggregation으로 DB에서 바로 집계하여 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AggregatedWordEntry.class))))
    })
    @GetMapping("/{membername}/level")
    public List<AggregatedWordEntry> getAllByLevelAgg(
            @Parameter(description = "사용자 이름", example = "user1", required = true)
            @PathVariable String membername,
            @Parameter(description = "난이도 (A1~C2 등)", example = "b1")
            @RequestParam(required = false) String level,
            @Parameter(description = "언어 코드(en/ko). 미지정 시 전체", example = "en")
            @RequestParam(required = false) String lang,
            @Parameter(description = "상위 N개 제한. 미지정 시 전체", example = "100")
            @RequestParam(required = false) Integer limit
    ) {
        return memberVocaBookService.getAllByLevelAgg(membername, level, lang, limit);
    }
}