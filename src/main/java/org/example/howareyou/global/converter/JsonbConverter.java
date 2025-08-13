package org.example.howareyou.global.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.AttributeConverter;
import org.example.howareyou.domain.notification.entity.NotificationType;
import org.example.howareyou.domain.notification.entity.payload.ChatPayload;
import org.example.howareyou.domain.notification.entity.payload.NotificationPayload;

public class JsonbConverter implements AttributeConverter<NotificationPayload, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(NotificationPayload attribute) {
        try {
            ObjectNode node = objectMapper.valueToTree(attribute);
            node.put("type", attribute.getType().name());  // ✅ 명시적으로 타입 포함
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalArgumentException("Payload 직렬화 실패", e);
        }
    }

    @Override
    public NotificationPayload convertToEntityAttribute(String dbData) {
        try {
            JsonNode root = objectMapper.readTree(dbData);
            JsonNode typeNode = root.get("type");

            if (typeNode == null || typeNode.isNull()) {
                throw new IllegalArgumentException("Missing 'type' in payload JSON");
            }

            String type = typeNode.asText();
            NotificationType notificationType = NotificationType.valueOf(type);

            return switch (notificationType) {
                case CHAT -> objectMapper.treeToValue(root, ChatPayload.class);
                default -> throw new IllegalArgumentException("Unsupported payload type: " + type);
            };
        } catch (Exception e) {
            throw new IllegalArgumentException("Payload 역직렬화 실패", e);
        }
    }
}