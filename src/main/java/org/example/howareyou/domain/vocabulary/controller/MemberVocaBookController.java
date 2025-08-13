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
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
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
        log.info("📖 전체 사용자 모든 단어장 조회 요청");
        return memberVocaBookService.findAll();
    }

    /**
     * 사용자별 단어장 전체 조회
     */
    @Operation(
            summary = "사용자 전체 단어(중복 제거, 최신 기준) 조회",
            description = "사용자의 모든 날짜 문서를 합쳐 (word,pos) 중복을 제거하고 analyzedAt이 가장 최신인 항목만 페이지 단위로 반환합니다."
    )
    @GetMapping("/{membername}")
    public ResponseEntity<Page<AggregatedWordEntry>> getLatestUniqueWords(
            @Parameter(description = "사용자 이름", example = "user1", required = true)
            @PathVariable String membername,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String pos,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Page<AggregatedWordEntry> result =
                memberVocaBookService.findLatestUniqueWordsPaged(membername, lang, pos, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * 특정 사용자의 날짜별 단어장 조회
     * - document ID: membername_yyyy-MM-dd
     */
    @Operation(
            summary = "특정 사용자의 날짜별 단어장 조회",
            description = "지정한 날짜(yyyy-MM-dd)의 단어장을 조회하고, 단어(words) 배열을 페이지 단위로 반환"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "단어장을 찾을 수 없음")
    })
    @GetMapping("/{membername}/{date}")
    public ResponseEntity<Page<MemberVocabulary.MemberWordEntry>> getVocabulariesByMemberAndDate(
            @Parameter(description = "사용자 이름", example = "user1", required = true)
            @PathVariable String membername,
            @Parameter(description = "단어 집계 시작 날짜 (yyyy-MM-dd)", example = "2025-08-12", required = true)
            @PathVariable String date,
            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "50")
            @RequestParam(defaultValue = "50") int size,
            @Parameter(description = "정렬 기준 (word|analyzedAt)", example = "analyzedAt")
            @RequestParam(defaultValue = "analyzedAt") String sortBy,
            @Parameter(description = "정렬 방향 (asc|desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String direction
    ) {
        LocalDate localDate = LocalDate.parse(date); // yyyy-MM-dd

        try {
            Page<MemberVocabulary.MemberWordEntry> pageResult =
                    memberVocaBookService.findWordsByMemberAndDatePaged(membername, localDate, page, size, sortBy, direction);
            return ResponseEntity.ok(pageResult);
        } catch (CustomException e) {
            // 서비스에서 VOCABULARY_NOT_FOUND 던질 때 404로 매핑
            if (e.getErrorCode() == ErrorCode.VOCABULARY_NOT_FOUND) {
                return ResponseEntity.notFound().build();
            }
            throw e; // 다른 에러는 전파
        }
    }

    /**
     * 사용자별 단어장 난이도별 조회
     */
    @Operation(
            summary = "사용자별 단어장 난이도별 조회",
            description = "사용자의 모든 날짜 문서를 합쳐 (word,pos) 중복을 제거하고 analyzedAt이 가장 최신인 항목만 페이지 단위로 반환합니다. level은 경로 변수로 받습니다."
    )
    @GetMapping("/{membername}/level/{level}")
    public ResponseEntity<Page<AggregatedWordEntry>> getLatestUniqueWordsByLevel(
            @Parameter(description = "사용자 이름", example = "user1", required = true)
            @PathVariable String membername,
            @Parameter(description = "레벨(en:a1,a2,b1,b2,c1 / ko:A,B,C)", example = "a1", required = true)
            @PathVariable String level,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String pos,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Page<AggregatedWordEntry> result =
                memberVocaBookService.findLatestUniqueWordsByLevelPaged(membername, lang, pos, level, page, size);
        return ResponseEntity.ok(result);
    }
}