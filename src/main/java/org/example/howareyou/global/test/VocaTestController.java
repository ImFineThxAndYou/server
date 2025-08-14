package org.example.howareyou.global.test;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageStatus;
import org.example.howareyou.domain.chat.websocket.repository.ChatMessageDocumentRepository;
import org.example.howareyou.domain.vocabulary.dto.AnalyzeRequestDto;
import org.example.howareyou.domain.vocabulary.dto.AnalyzedResponseWord;
import org.example.howareyou.domain.vocabulary.service.ChatVocaBookService;
import org.example.howareyou.domain.vocabulary.service.MemberVocaBookService;
import org.example.howareyou.domain.vocabulary.service.NlpClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private final ChatVocaBookService chatVocaBookService;
    private final MemberVocaBookService memberVocaBookService;
    private final NlpClient nlpClient;


    @Operation(
            summary = "채팅 메시지 NLP 분석",
            description = "주어진 텍스트를 NLP 서버로 전송하여 불용어 제거 및 품사 태깅된 단어 목록을 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "분석 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AnalyzedResponseWord.class)))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/analyze/chats")
    public Mono<ResponseEntity<List<AnalyzedResponseWord>>> analyzeText(
            @Parameter(description = "분석할 텍스트", required = true)
            @RequestBody AnalyzeRequestDto request) {
        return nlpClient.analyzeReactive(request.getText())
                .map(ResponseEntity::ok);
    }


    @Operation(
            summary = "테스트용 채팅 메시지 생성",
            description = "채팅방 UUID, 발신자, 메시지 내용, 시간 등을 포함하여 다건 테스트 채팅 데이터를 MongoDB에 저장합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채팅 메시지 생성 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 오류", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
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

    //채팅창 별 단어장 생성 - 비동기 버전
    @Operation(
            summary = "단어장 생성 배치 실행 (비동기, 테스트)",
            description = "주어진 시간 범위를 NLP 분석해 채팅방 단어장을 생성합니다. 미지정 시 직전 1시간."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "요청 접수(비동기 시작)"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/generate-vocabook-reactive")
    public ResponseEntity<Map<String, Object>> testGenerateVocabulary(
            @Parameter(description = "시작 시간 (ISO-8601)", example = "2025-08-06T07:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @Parameter(description = "종료 시간 (ISO-8601)", example = "2025-08-06T11:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        Map<String, Object> result = new HashMap<>();

        Instant now = Instant.now();
        Instant from = start != null ? start : now.minus(1, ChronoUnit.HOURS);
        Instant to = end != null ? end : now;

        chatVocaBookService.generateVocabularyForRangeReactive(from, to)
                .subscribe(
                        null,
                        ex -> log.error("단어장 생성 실패", ex),
                        () -> log.info("단어장 생성 완료: {} ~ {}", from, to)
                );

        result.put("success", true);
        result.put("message", "단어장 생성 요청이 비동기로 시작되었습니다.");
        result.put("start", from.toString());
        result.put("end", to.toString());

        return ResponseEntity.ok(result);
    }

    /**
     * 단일 사용자에 대해 단어장 생성 로직 테스트
     */
    @PostMapping("/generate-for-member")
    public ResponseEntity<Map<String, Object>> generateForMember(
            @Parameter(description = "사용자 ID", example = "3", required = true)
            @RequestParam Long memberId,

            @Parameter(description = "사용자 membername", example = "user1", required = true)
            @RequestParam String membername,

            @Parameter(description = "사용자 언어(ko/en) — 반대 언어가 수집 대상", example = "ko", required = true)
            @RequestParam String userLang,

            @Parameter(description = "시작 시간 (ISO-8601, UTC)", example = "2025-08-12T00:00:00Z")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,

            @Parameter(description = "종료 시간 (ISO-8601, UTC)", example = "2025-08-13T00:00:00Z")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,

            @Parameter(description = "타임존 (start/end 미지정 시 사용)", example = "Asia/Seoul")
            @RequestParam(required = false, defaultValue = "Asia/Seoul") String timezone
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            Instant from, to;
            LocalDate yesterLocalDate;
            ZoneId zone = ZoneId.of(timezone);

            if (start != null && end != null) {
                from = start;
                to = end;
                // 시작 시간을 사용자 타임존으로 변환해서 날짜 추출
                yesterLocalDate = start.atZone(zone).toLocalDate();
            } else {
                ZonedDateTime now = ZonedDateTime.now(zone);
                ZonedDateTime startZdt = now.minusDays(1).toLocalDate().atStartOfDay(zone);
                ZonedDateTime endZdt = startZdt.plusDays(1);
                from = startZdt.toInstant();
                to = endZdt.toInstant();
                yesterLocalDate = startZdt.toLocalDate();
            }

            // ✅ docId를 여기서 미리 생성
            String docId = membername + "_" + yesterLocalDate.toString();

            log.info("▶️ 테스트 실행: memberId={}, membername={}, userLang={}, docId={}, range={}~{}, tz={}",
                    memberId, membername, userLang, docId, from, to, timezone);

            // ✅ 서비스 호출 시 docId를 함께 전달
            memberVocaBookService.generateVocabularyForMember(memberId, membername, userLang, from, to, docId);

            result.put("success", true);
            result.put("message", "사용자 단어장 생성 로직 실행 완료");
            result.put("memberId", memberId);
            result.put("membername", membername);
            result.put("userLang", userLang);
            result.put("docId", docId);
            result.put("start", from.toString());
            result.put("end", to.toString());
            result.put("timezone", timezone);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("사용자 단어장 생성 중 오류", e);
            result.put("success", false);
            result.put("message", "오류 발생: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }


}