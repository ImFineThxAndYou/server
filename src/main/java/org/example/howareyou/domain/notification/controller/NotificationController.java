package org.example.howareyou.domain.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.notification.entity.Notification;
import org.example.howareyou.domain.notification.redis.RedisEmitter;
import org.example.howareyou.domain.notification.repository.NotificationRepository;
import org.example.howareyou.domain.notification.service.NotificationPushService;
import org.example.howareyou.global.security.CustomMemberDetails;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

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
            throw new RuntimeException("인증이 필요합니다.");
        }

        Long memberId = memberDetails.getId();
        String membername = memberDetails.getMembername();
        
        log.info("SSE 연결 성공: memberId={}, membername={}", memberId, membername);
        
        SseEmitter emitter = emitters.add(memberId);

        // 구독 직후 미전송 backlog push
        pushService.pushUndelivered(memberId, emitter);

        return emitter;
    }

    /** 하트비트 응답 엔드포인트 */
    @PostMapping("/heartbeat")
    public void heartbeat(@AuthenticationPrincipal CustomMemberDetails memberDetails) {
        emitters.touch(memberDetails.getId());  // Redis TTL 연장
    }
}