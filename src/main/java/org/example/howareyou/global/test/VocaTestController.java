package org.example.howareyou.global.test;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.example.howareyou.domain.chat.websocket.entity.ChatMessageStatus;
import org.example.howareyou.domain.chat.websocket.repository.ChatMessageDocumentRepository;
import org.example.howareyou.domain.vocabulary.service.ChatVocaBookService;
import org.example.howareyou.domain.vocabulary.service.MemberVocaBookService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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

    //단어장 생성 로직을 테스트
    @Operation(
            summary = "단어장 생성 배치 실행 (테스트)",
            description = "주어진 시간 범위에 포함된 채팅 메시지를 기반으로 NLP 분석 후 MongoDB에 단어장 데이터를 저장합니다. 파라미터 생략 시 기본적으로 직전 1시간 범위로 분석합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "단어장 생성 성공"),
            @ApiResponse(responseCode = "500", description = "단어장 생성 중 서버 오류")
    })
    @PostMapping("/generate-vocabook")
    public ResponseEntity<Map<String, Object>> testGenerateVocabulary(
            @Parameter(description = "시작 시간 (ISO-8601 형식)", example = "2025-08-05T04:00:00Z")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,

            @Parameter(description = "종료 시간 (ISO-8601 형식)", example = "2025-08-07T05:00:00Z")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        Map<String, Object> result = new HashMap<>();

        try {
            Instant now = Instant.now();
            Instant from = start != null ? start : now.minus(1, ChronoUnit.HOURS);
            Instant to = end != null ? end : now;

            chatVocaBookService.generateVocabularyForLastHour(from, to);

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

    /**
     * 단일 사용자에 대해 단어장 생성 로직 테스트
     */
    @Operation(
            summary = "사용자 단어장 생성 실행 (테스트)",
            description = """
                    특정 사용자에 대해 단어장 생성 로직을 실행합니다.
                    - start/end 미지정 시, timezone 기준 '어제 00:00 ~ 오늘 00:00'을 자동 계산합니다.
                    - userLang은 'ko' 또는 'en' (반대 언어가 수집 대상)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 실행 성공"),
            @ApiResponse(responseCode = "500", description = "실행 중 서버 오류", content = @Content)
    })
    @PostMapping("/generate-for-member")
    public ResponseEntity<Map<String, Object>> generateForMember(
            @Parameter(description = "사용자 ID", example = "3", required = true)
            @RequestParam Long memberId,

            @Parameter(description = "사용자 membername", example = "user1", required = true)
            @RequestParam String membername,

            @Parameter(description = "사용자 언어(ko/en) — 반대 언어가 수집 대상", example = "ko", required = true)
            @RequestParam String userLang,

            @Parameter(description = "시작 시간 (ISO-8601, UTC)", example = "2025-08-08T00:00:00Z")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,

            @Parameter(description = "종료 시간 (ISO-8601, UTC)", example = "2025-08-09T00:00:00Z")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,

            @Parameter(description = "타임존 (start/end 미지정 시 사용)", example = "Asia/Seoul")
            @RequestParam(required = false, defaultValue = "Asia/Seoul") String timezone
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 시간 범위 계산: start/end 없으면 timezone 기준 어제 하루
            Instant from, to;
            if (start != null && end != null) {
                from = start;
                to = end;
            } else {
                ZoneId zone = ZoneId.of(timezone);
                ZonedDateTime now = ZonedDateTime.now(zone);
                ZonedDateTime startZdt = now.minusDays(1).toLocalDate().atStartOfDay(zone);
                ZonedDateTime endZdt = startZdt.plusDays(1);
                from = startZdt.toInstant();
                to = endZdt.toInstant();
            }

            log.info("▶️ 테스트 실행: memberId={}, membername={}, userLang={}, range={}~{}, tz={}",
                    memberId, membername, userLang, from, to, timezone);

            memberVocaBookService.generateVocabularyForMember(memberId, membername, userLang, from, to);

            result.put("success", true);
            result.put("message", "사용자 단어장 생성 로직 실행 완료");
            result.put("memberId", memberId);
            result.put("membername", membername);
            result.put("userLang", userLang);
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