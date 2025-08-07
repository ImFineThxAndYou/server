package org.example.howareyou.domain.notification.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
    import lombok.*;
import org.example.howareyou.domain.notification.entity.payload.ChatPayload;
import org.example.howareyou.domain.notification.entity.payload.NotificationPayload;
import org.example.howareyou.global.converter.JsonbConverter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Convert(converter = JsonbConverter.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private NotificationPayload payload;  // ✅ 인터페이스 타입

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant deliveredAt;
    private Instant readAt;

    // 상태 전환
    public void markDelivered() { this.deliveredAt = Instant.now(); }
    public void markRead() { this.readAt = Instant.now(); }
    public boolean isUndelivered() { return deliveredAt == null; }
    public boolean isUnread() { return readAt == null; }

    // 팩토리 메서드
    // 팩토리 메서드
    public static Notification chat(Long receiverId, Long roomId, Long senderId, String preview) {
        ChatPayload payload = new ChatPayload(roomId, senderId, preview);
        return Notification.builder()
                .receiverId(receiverId)
                .type(NotificationType.CHAT)
                .payload(payload)
                .createdAt(Instant.now())
                .build();
    }
}