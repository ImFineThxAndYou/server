package org.example.howareyou.domain.notification.repository;


import org.example.howareyou.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n WHERE n.receiverId = :receiverId AND n.deliveredAt IS NULL")
    List<Notification> findUndeliveredByReceiverId(@Param("receiverId") Long receiverId);

//    /** 최근 30건 조회 */
//    List<Notification> findTop30ByReceiverIdAndCreatedAtAfterOrderByCreatedAtDesc(
//            Long receiverId, Instant after);
}