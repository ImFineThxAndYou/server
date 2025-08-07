package org.example.howareyou.domain.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.notification.entity.Notification;
import org.example.howareyou.domain.notification.redis.RedisEmitter;
import org.example.howareyou.domain.notification.repository.NotificationRepository;
import org.example.howareyou.domain.notification.service.NotificationPushService;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.example.howareyou.global.security.CustomMemberDetails;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/notify")
@Slf4j
public class NotificationController {

    private final RedisEmitter emitters;
    private final MemberService memberService;           // name ↔ id
    private final NotificationPushService pushService;

    /** 클라이언트 구독 엔드포인트 */
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
            log.warn("즉시 ping 전송 실패: memberId={}", memberId);
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

    /** 하트비트 응답 엔드포인트 */
    @PostMapping("/heartbeat")
    public void heartbeat(@AuthenticationPrincipal CustomMemberDetails memberDetails) {
        emitters.touch(memberDetails.getId());  // Redis TTL 연장
    }
}