package org.example.howareyou.domain.vocabulary.controller;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.example.howareyou.domain.chat.websocket.service.ChatMessageService;
import org.example.howareyou.domain.vocabulary.dto.AnalyzeRequestDto;
import org.example.howareyou.domain.vocabulary.dto.AnalyzedResponseWord;
import org.example.howareyou.domain.vocabulary.service.NlpClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/vocabook")
public class VocaBookController {
    private final NlpClient nlpClient;
    private final ChatMessageService chatMessageService;

    @PostMapping("/analyze/chats")
    public ResponseEntity<List<AnalyzedResponseWord>> analyzeText(@RequestBody AnalyzeRequestDto request) {
        List<AnalyzedResponseWord> result = nlpClient.analyze(request.getText());
        return ResponseEntity.ok(result);
    }

}