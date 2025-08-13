package org.example.howareyou.domain.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;

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
    public static Notification chat(Long receiverId, Long roomId, Long senderId,String messageId, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("chatRoomId", roomId);
        payload.put("senderId", senderId);
        payload.put("messageId", messageId);
        payload.put("message", message);
        
        return Notification.builder()
                .receiverId(receiverId)
                .type(NotificationType.CHAT)
                .payload(payload)
                .createdAt(Instant.now())
                .build();
    }
    
    public static Notification chatReq(Long receiverId, Long requesterId, String requesterName, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requesterId", requesterId);
        payload.put("requesterName", requesterName);
        payload.put("message", message);
        
        return Notification.builder()
                .receiverId(receiverId)
                .type(NotificationType.CHATREQ)
                .payload(payload)
                .createdAt(Instant.now())
                .build();
    }
    
    public static Notification system(Long receiverId, String title, String content, String category) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("content", content);
        payload.put("category", category); // "notice", "event", "maintenance" 등
        
        return Notification.builder()
                .receiverId(receiverId)
                .type(NotificationType.SYSTEM)
                .payload(payload)
                .createdAt(Instant.now())
                .build();
    }
}