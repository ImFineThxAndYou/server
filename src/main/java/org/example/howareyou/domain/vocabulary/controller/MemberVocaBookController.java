package org.example.howareyou.domain.vocabulary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.vocabulary.document.MemberVocabulary;
import org.example.howareyou.domain.vocabulary.service.MemberVocaBookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/vocabook/member")
@RequiredArgsConstructor
@Tag(name = "개인 단어장", description = "사용자별 개인 단어장 조회 API")
public class MemberVocaBookController {

    private final MemberVocaBookService memberVocaBookService;

    @Operation(
        summary = "전체 사용자 단어장 목록 조회",
        description = "모든 사용자의 단어장 목록을 조회합니다. (관리자용)"
    )
    @GetMapping("/all")
    public List<MemberVocabulary> getAllMemberVocabularies() {
        log.info("📖 전체 사용자 단어장 조회 요청");
        return memberVocaBookService.findAll();
    }

    @Operation(
        summary = "특정 사용자의 전체 단어장 목록 조회",
        description = "특정 사용자가 학습한 모든 날짜의 단어장 목록을 조회합니다."
    )
    @GetMapping("/{membername}")
    public List<MemberVocabulary> getVocabulariesByMember(
            @Parameter(description = "사용자명", required = true, example = "john_doe")
            @PathVariable String membername
    ) {
        log.info("📖 사용자 [{}] 단어장 목록 조회", membername);
        return memberVocaBookService.findByMembername(membername);
    }

    @Operation(
        summary = "특정 사용자의 날짜별 단어장 조회",
        description = "특정 사용자가 특정 날짜에 학습한 단어장을 조회합니다. " +
                     "날짜는 yyyy-MM-dd 형식으로 입력해야 합니다."
    )
    @GetMapping("/{membername}/{date}")
    public MemberVocabulary getVocabulariesByMemberAndDate(
            @Parameter(description = "사용자명", required = true, example = "john_doe")
            @PathVariable String membername,
            @Parameter(description = "날짜 (yyyy-MM-dd 형식)", required = true, example = "2024-01-15")
            @PathVariable String date
    ) {
        log.info("📖 사용자 [{}]의 [{}] 날짜 단어장 조회", membername, date);
        LocalDate localDate = LocalDate.parse(date); // yyyy-MM-dd 형식
        return memberVocaBookService.findByMembernameAndDate(membername, localDate);
    }
}