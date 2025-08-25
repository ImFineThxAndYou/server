package org.example.howareyou.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.notification.entity.Notification;
import org.example.howareyou.domain.notification.redis.RedisEmitter;
import org.example.howareyou.domain.notification.repository.NotificationRepository;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPushService {

    private final NotificationRepository notificationRepository;
    private final RedisEmitter emitters;

    /** 채팅 알림 발송 */
    @Async("notificationExecutor")
    @Transactional
    public void sendChatNotify(Long roomId,
                               Long senderId, String messageId, String message,
                               Long receiverId) {

        log.info("채팅 알림 발송 시작: roomId={}, senderId={}, receiverId={}", roomId, senderId, receiverId);

        Notification n = notificationRepository.save(
                Notification.chat(receiverId, roomId, senderId, messageId, message));

        log.info("알림 저장 완료: notificationId={}", n.getId());

        // 로컬 메모리에서 SseEmitter 확인
        var emitterOpt = emitters.get(receiverId);
        log.info("SseEmitter 조회 결과: receiverId={}, found={}", receiverId, emitterOpt.isPresent());

        emitterOpt.ifPresentOrElse(
                emitter -> {
                    log.info("SseEmitter 발견, 전송 시도: receiverId={}", receiverId);
                    trySend(n, emitter);
                },
                () -> {
                    // Redis에서 온라인 상태 확인
                    boolean isOnlineInRedis = emitters.isOnline(receiverId);
                    log.info("SseEmitter 없음, Redis 상태 확인: receiverId={}, isOnlineInRedis={}", receiverId, isOnlineInRedis);

                    if (isOnlineInRedis) {
                        log.warn("Receiver {} is online in Redis but no local emitter found", receiverId);
                    } else {
                        log.info("Receiver {} offline, stored only", receiverId);
                    }
                }
        );
    }

    /** 채팅 요청 알림 발송 */
    @Transactional
    public void sendChatReqNotify(Long requesterId, String requesterName, String message, Long receiverId) {
        Notification n = notificationRepository.save(
                Notification.chatReq(receiverId, requesterId, requesterName, message));

        emitters.get(receiverId).ifPresentOrElse(
                emitter -> trySend(n, emitter),
                () -> {
                    if (emitters.isOnline(receiverId)) {
                        log.warn("Receiver {} is online in Redis but no local emitter found", receiverId);
                    } else {
                        log.info("Receiver {} offline, stored only", receiverId);
                    }
                }
        );
    }

    /** 시스템 알림 발송 (공지사항/이벤트) */
    @Transactional
    public void sendSystemNotify(String title, String content, String category, Long receiverId) {
        Notification n = notificationRepository.save(
                Notification.system(receiverId, title, content, category));

        emitters.get(receiverId).ifPresentOrElse(
                emitter -> trySend(n, emitter),
                () -> {
                    if (emitters.isOnline(receiverId)) {
                        log.warn("Receiver {} is online in Redis but no local emitter found", receiverId);
                    } else {
                        log.info("Receiver {} offline, stored only", receiverId);
                    }
                }
        );
    }

    @Transactional
    public void pushUndelivered(Long receiverId, SseEmitter emitter) {
        List<Notification> list = notificationRepository.findUndeliveredByReceiverId(receiverId);
        list.forEach(n -> trySend(n, emitter));
        notificationRepository.saveAll(list); // deliveredAt 업데이트 반영
    }

    /* ---------- 내부 util ---------- */
    private void trySend(Notification n, SseEmitter emitter) {
        try {
            log.debug("SSE 전송 시도: notificationId={}, type={}, receiverId={}",
                    n.getId(), n.getType(), n.getReceiverId());

            emitter.send(SseEmitter.event()
                    .id(String.valueOf(n.getId()))
                    .name(n.getType().name().toLowerCase())
                    .data(n.getPayload())); // payload는 Map<String, Object>이므로 Jackson이 자동으로 JSON으로 변환
            n.markDelivered();

            log.debug("SSE 전송 성공: notificationId={}", n.getId());
        } catch (IOException e) {
            log.warn("SSE send failed, will retry later: notificationId={}, error={}", n.getId(), e.getMessage());
            throw new CustomException(ErrorCode.NOTIFICATION_SEND_FAILED,
                    String.format("SSE 전송 실패: notificationId=%s, error=%s", n.getId(), e.getMessage()));
        }
    }

}