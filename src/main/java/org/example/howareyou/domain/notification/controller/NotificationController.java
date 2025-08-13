package org.example.howareyou.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.notification.dto.NotifyDto;
import org.example.howareyou.domain.notification.redis.RedisEmitter;
import org.example.howareyou.domain.notification.service.NotificationPushService;
import org.example.howareyou.domain.notification.service.NotificationService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.example.howareyou.global.security.CustomMemberDetails;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/notify")
@Slf4j
@Tag(name = "알림", description = "실시간 알림 및 SSE 관련 API")
public class NotificationController {

    private final RedisEmitter emitters;
    private final MemberService memberService;           // name ↔ id
    private final NotificationPushService pushService;
    private final NotificationService notificationService;

    @Operation(
        summary = "SSE 연결 구독",
        description = "Server-Sent Events를 통한 실시간 알림 구독을 시작합니다. " +
                     "연결 후 서버에서 ping 이벤트를 주기적으로 전송하며, " +
                     "클라이언트는 heartbeat 엔드포인트로 응답해야 합니다."
    )
    @GetMapping("/sse")
    public SseEmitter subscribe(@AuthenticationPrincipal CustomMemberDetails memberDetails) {

        // 인증된 사용자 정보 확인
        if (memberDetails == null) {
            throw new CustomException(ErrorCode.AUTH_TOKEN_EXPIRED, "SSE 연결을 위한 인증이 필요합니다.");
        }

        Long memberId = memberDetails.getId();
        String membername = memberDetails.getMembername();
        
        log.info("SSE 연결 시도: memberId={}, membername={}", memberId, membername);
        
        SseEmitter emitter = emitters.add(memberId);

        // 즉시 연결 확인을 위한 ping 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("ping")
                    .data("{}"));
            log.debug("즉시 ping 전송 완료: memberId={}", memberId);
        } catch (IOException e) {
            log.debug("즉시 ping 전송 실패 (클라이언트 연결 끊김): memberId={}, error={}", memberId, e.getMessage());
            emitters.remove(memberId);
            throw new CustomException(ErrorCode.SSE_CONNECTION_FAILED, 
                    String.format("SSE 연결 실패: memberId=%d, error=%s", memberId, e.getMessage()));
        }

        // 미전송 알림은 비동기로 처리 (연결 지연 방지)
        CompletableFuture.runAsync(() -> {
            try {
                pushService.pushUndelivered(memberId, emitter);
                log.debug("미전송 알림 처리 완료: memberId={}", memberId);
            } catch (Exception e) {
                log.warn("미전송 알림 처리 실패: memberId={}, error={}", memberId, e.getMessage());
            }
        });

        log.info("SSE 연결 성공: memberId={}, membername={}", memberId, membername);
        return emitter;
    }

    @Operation(
        summary = "하트비트 응답",
        description = "SSE 연결 유지를 위한 하트비트 응답을 전송합니다. " +
                     "서버에서 ping 이벤트를 받으면 이 엔드포인트로 응답하여 " +
                     "연결 상태를 유지해야 합니다."
    )
    @PostMapping("/heartbeat")
    public void heartbeat(@AuthenticationPrincipal CustomMemberDetails memberDetails) {
        emitters.touch(memberDetails.getId());  // Redis TTL 연장
    }

    @Operation(
        summary = "알림 목록 조회",
        description = "사용자의 알림 목록을 페이지네이션으로 조회합니다. " +
                     "최신 알림부터 정렬되어 반환됩니다."
    )
    @GetMapping
    public Page<NotifyDto> list(
            @AuthenticationPrincipal CustomMemberDetails me,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return notificationService.getNotifications(me.getId(), page, size);
    }

    @Operation(
        summary = "읽지 않은 알림 개수 조회",
        description = "사용자가 읽지 않은 알림의 개수를 반환합니다."
    )
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal CustomMemberDetails memberDetails) {
        return Map.of("unread", notificationService.unreadCount(memberDetails.getId()));
    }

    @Operation(
        summary = "알림 읽음 처리",
        description = "특정 알림을 읽음 상태로 변경합니다."
    )
    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @AuthenticationPrincipal CustomMemberDetails memberDetails, 
            @Parameter(description = "알림 ID (UUID)", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String id
    ) {
        UUID uuid = UUID.fromString(id);
        notificationService.markRead(memberDetails.getId(), uuid);
    }

}