package org.example.howareyou.domain.vocabulary.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.vocabulary.document.MemberVocabulary;
import org.example.howareyou.domain.vocabulary.service.MemberVocaBookService;
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
     * 📌 전체 사용자 단어장 목록 조회
     */
    @GetMapping("/all")
    public List<MemberVocabulary> getAllMemberVocabularies() {
        log.info("📖 전체 사용자 단어장 조회 요청");
        return memberVocaBookService.findAll();
    }

    /**
     * 📌 특정 사용자의 전체 단어장 목록 조회
     */
    @GetMapping("/{membername}")
    public List<MemberVocabulary> getVocabulariesByMember(@PathVariable String membername) {
        log.info("📖 사용자 [{}] 단어장 목록 조회", membername);
        return memberVocaBookService.findByMembername(membername);
    }

    /**
     * 📌 특정 사용자의 날짜별 단어장 조회
     * - document ID: membername_yyyy-MM-dd
     */
    @GetMapping("/{membername}/{date}")
    public MemberVocabulary getVocabulariesByMemberAndDate(
            @PathVariable String membername,
            @PathVariable String date
    ) {
        log.info("📖 사용자 [{}]의 [{}] 날짜 단어장 조회", membername, date);
        LocalDate localDate = LocalDate.parse(date); // yyyy-MM-dd 형식
        return memberVocaBookService.findByMembernameAndDate(membername, localDate);
    }
}