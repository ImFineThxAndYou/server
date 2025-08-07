package org.example.howareyou.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.notification.entity.Notification;
import org.example.howareyou.domain.notification.redis.RedisEmitter;
import org.example.howareyou.domain.notification.repository.NotificationRepository;
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

    /** 채팅 알림 발송 예시 */
    @Transactional
    public void sendChatNotify(Long roomId,
                               Long senderId, String preview,
                               Long receiverId /* resolve beforehand */) {

        Notification n = notificationRepository.save(
                Notification.chat(receiverId, roomId, senderId, preview));

        emitters.get(receiverId).ifPresentOrElse(
                emitter -> trySend(n, emitter),
                () -> log.info("Receiver {} offline, stored only", receiverId)
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
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(n.getId()))
                    .name(n.getType().name().toLowerCase())
                    .data(n.getPayload()));
            n.markDelivered();
        } catch (IOException e) {
            log.warn("SSE send failed, will retry later: {}", n.getId());
        }
    }

}