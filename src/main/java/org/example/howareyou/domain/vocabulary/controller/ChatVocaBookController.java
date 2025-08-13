package org.example.howareyou.domain.vocabulary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.vocabulary.document.ChatRoomVocabulary;
import org.example.howareyou.domain.vocabulary.dto.AnalyzeRequestDto;
import org.example.howareyou.domain.vocabulary.dto.AnalyzedResponseWord;
import org.example.howareyou.domain.vocabulary.service.NlpClient;
import org.example.howareyou.domain.vocabulary.service.ChatVocaBookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vocabook")
public class ChatVocaBookController {
    private final ChatVocaBookService chatVocaBookService;

    /**
     * 특정 채팅방의 전체 단어장 목록 조회 (최신순)
     * 더미 데이터로 room1234, room5678 있음
     */
    @Operation(
            summary = "특정 채팅방의 단어장 목록 조회",
            description = "채팅방 UUID를 기준으로, 해당 채팅방에서 생성된 모든 단어장 목록을 최신순으로 반환합니다.\n\n예시: room1234, room5678"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "단어장 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 채팅방의 단어장이 존재하지 않음")
    })
    @GetMapping("/{chatRoomUuid}")
    public ResponseEntity<List<ChatRoomVocabulary>> getVocabularyListByChatRoom(
            @Parameter(description = "채팅방 UUID", example = "room1234")
            @PathVariable String chatRoomUuid
    ) {
        List<ChatRoomVocabulary> vocabList = chatVocaBookService.getAllVocabularyByChatRoom(chatRoomUuid);
        return ResponseEntity.ok(vocabList);
    }

    /**
     * 전체 채팅방의 전체 단어장 목록 조회 (최신순)
     */
    @Operation(
            summary = "전체 단어장 목록 조회",
            description = "모든 채팅방의 단어장 목록을 최신순으로 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "전체 단어장 목록 조회 성공")
    })
    @GetMapping("/all")
    public ResponseEntity<List<ChatRoomVocabulary>> getAllVocabularies() {
        List<ChatRoomVocabulary> allVocabularies = chatVocaBookService.getAllVocabularies();
        return ResponseEntity.ok(allVocabularies);
    }

}