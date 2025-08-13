package org.example.howareyou.domain.vocabulary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.vocabulary.document.ChatRoomVocabulary;
import org.example.howareyou.domain.vocabulary.dto.AnalyzeRequestDto;
import org.example.howareyou.domain.vocabulary.dto.AnalyzedResponseWord;
import org.example.howareyou.domain.vocabulary.service.ChatVocaBookService;
import org.example.howareyou.domain.vocabulary.service.NlpClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vocabook")
@Tag(name = "채팅 단어장", description = "채팅방 기반 단어장 및 텍스트 분석 API")
public class ChatVocaBookController {
    private final NlpClient nlpClient;
    private final ChatVocaBookService chatVocaBookService;

    @Operation(
        summary = "텍스트 분석",
        description = "입력된 텍스트를 분석하여 학습 가능한 단어들을 추출합니다. " +
                     "NLP 엔진을 사용하여 문맥에 맞는 단어와 의미를 제공합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "텍스트 분석 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/analyze/chats")
    public ResponseEntity<List<AnalyzedResponseWord>> analyzeText(
            @Parameter(description = "분석할 텍스트", required = true)
            @RequestBody AnalyzeRequestDto request
    ) {
        List<AnalyzedResponseWord> result = nlpClient.analyze(request.getText());
        return ResponseEntity.ok(result);
    }

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