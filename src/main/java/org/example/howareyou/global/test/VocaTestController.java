package org.example.howareyou.global.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageStatus;
import org.example.howareyou.domain.chat.websocket.repository.ChatMessageDocumentRepository;
import org.example.howareyou.domain.vocabulary.service.VocaBookService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/voca")
public class VocaTestController {

    private final ChatMessageDocumentRepository chatMessageDocumentRepository;
    private final VocaBookService vocaBookService;

    @PostMapping("/create-chats")
    public ResponseEntity<Map<String, Object>> createTestChats(@RequestBody List<Map<String, Object>> requests) {
        Map<String, Object> result = new HashMap<>();
        List<ChatMessageDocument> messagesToSave = new ArrayList<>();

        try {
            for (Map<String, Object> req : requests) {
                String chatRoomUuid = (String) req.get("chatRoomUuid");
                String sender = (String) req.get("sender");
                String content = (String) req.get("content");
                String status = (String) req.getOrDefault("status", "UNREAD");
                String isoTime = (String) req.getOrDefault("messageTime", Instant.now().toString());

                if (chatRoomUuid == null || sender == null || content == null) {
                    log.warn("필수값 누락으로 건너뜀: {}", req);
                    continue;
                }

                ChatMessageDocument message = ChatMessageDocument.builder()
                        .chatRoomUuid(chatRoomUuid)
                        .sender(sender)
                        .content(content)
                        .messageTime(Instant.parse(isoTime))
                        .chatMessageStatus(ChatMessageStatus.valueOf(status))
                        .build();

                messagesToSave.add(message);
            }

            chatMessageDocumentRepository.saveAll(messagesToSave);

            result.put("success", true);
            result.put("count", messagesToSave.size());
            result.put("message", messagesToSave.size() + "개 채팅 메시지 생성 완료");
        } catch (Exception e) {
            log.error("다건 테스트 채팅 생성 중 오류", e);
            result.put("success", false);
            result.put("message", "오류: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    //단어장 생성 로직을 테스트
    @PostMapping("/generate-vocabook")
    public ResponseEntity<Map<String, Object>> testGenerateVocabulary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        Map<String, Object> result = new HashMap<>();

        try {
            Instant now = Instant.now();
            Instant from = start != null ? start : now.minus(1, ChronoUnit.HOURS);
            Instant to = end != null ? end : now;

            vocaBookService.generateVocabularyForLastHour(from, to);

            result.put("success", true);
            result.put("message", "단어장 생성 로직 실행 완료");
            result.put("start", from.toString());
            result.put("end", to.toString());
        } catch (Exception e) {
            log.error("단어장 생성 중 오류", e);
            result.put("success", false);
            result.put("message", "오류 발생: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }
}