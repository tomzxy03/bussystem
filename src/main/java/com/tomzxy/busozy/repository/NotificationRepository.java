package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.common.enums.NotificationStatus;
import com.tomzxy.busozy.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n FROM Notification n
            WHERE n.user.id = :userId
              AND (:statuses IS NULL OR n.status IN :statuses)
            ORDER BY n.createdAt DESC
            """)
    Page<Notification> findByUserIdAndStatuses(
            @Param("userId") Long userId,
            @Param("statuses") Collection<NotificationStatus> statuses,
            Pageable pageable);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE n.user.id = :userId
              AND n.status <> 'READ'
            """)
    long countUnreadByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.status = 'READ',
                n.readAt = :readAt
            WHERE n.id = :id
              AND n.user.id = :userId
              AND n.status <> 'READ'
            """)
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId, @Param("readAt") OffsetDateTime readAt);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.status = 'READ',
                n.readAt = :readAt
            WHERE n.user.id = :userId
              AND n.status <> 'READ'
            """)
    int markAllAsRead(@Param("userId") Long userId, @Param("readAt") OffsetDateTime readAt);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.status IN ('PENDING', 'FAILED')
              AND n.retryCount < :maxRetry
            ORDER BY n.createdAt ASC
            """)
    List<Notification> findRetryCandidates(@Param("maxRetry") int maxRetry);
}
