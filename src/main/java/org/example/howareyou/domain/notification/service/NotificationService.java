package org.example.howareyou.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.notification.dto.NotifyDto;
import org.example.howareyou.domain.notification.repository.NotificationRepository;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Page<NotifyDto> getNotifications(Long receiverId, int page, int size) {
        validateReceiver(receiverId);
        PageRequest pr = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),         // size 1~50 가드
                Sort.by(Sort.Direction.DESC, "createdAt") // 최신순
        );

        // 엔터티 → DTO 매핑 (필요 필드만)
        return notificationRepository.findByReceiverId(receiverId, pr)
                .map(n -> new NotifyDto(
                        n.getId().toString(),
                        n.getType(),
                        n.getReadAt(),
                        n.getCreatedAt(),
                        n.getPayload().toString()
                ));
    }

    public long unreadCount(Long receiverId) {
        validateReceiver(receiverId);
        return notificationRepository.countByReceiverIdAndReadAtIsNull(receiverId);
    }

    @Transactional
    public void markRead(Long receiverId, UUID id) {
        validateReceiver(receiverId);
        int updated = notificationRepository.markAsRead(receiverId, id);
        if (updated == 0) {
            // 이미 읽었거나 남의 알림이거나 존재X
            throw new CustomException(
                    ErrorCode.NOTIFICATION_NOT_FOUND,
                    "읽음 처리할 알림이 없습니다. id=" + id
            );
        }
    }

    private void validateReceiver(Long receiverId) {
        if (receiverId == null) {
            throw new CustomException(ErrorCode.NOTIFICATION_RECEIVER_NOT_FOUND, "receiverId=null");
        }
    }
}

