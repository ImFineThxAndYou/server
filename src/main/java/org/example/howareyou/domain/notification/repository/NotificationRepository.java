package org.example.howareyou.domain.notification.repository;


import org.example.howareyou.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n WHERE n.receiverId = :receiverId AND n.deliveredAt IS NULL")
    List<Notification> findUndeliveredByReceiverId(@Param("receiverId") Long receiverId);

    List<Notification> findAllByReceiverId(Long receiverId);

    Long countByReadAtIsNullAndReceiverId(Long receiverId);


    Page<Notification> findByReceiverId(Long receiverId, Pageable pageable);

    // 미읽음 카운트
    long countByReceiverIdAndReadAtIsNull(Long receiverId);

    // 단건 읽음(멱등): 아직 안 읽은 경우에만 timestamp 세팅
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Notification n
           set n.readAt = CURRENT_TIMESTAMP
         where n.receiverId = :receiverId
           and n.id = :id
           and n.readAt is null
    """)
    int markAsRead(@Param("receiverId") Long receiverId, @Param("id") UUID id);

//    /** 최근 30건 조회 */
//    List<Notification> findTop30ByReceiverIdAndCreatedAtAfterOrderByCreatedAtDesc(
//            Long receiverId, Instant after);
}