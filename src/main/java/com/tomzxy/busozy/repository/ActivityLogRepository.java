package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    @EntityGraph(attributePaths = "user")
    @Query("""
            SELECT l FROM ActivityLog l
            WHERE (:action IS NULL OR l.action = :action)
              AND (:entityType IS NULL OR l.entityType = :entityType)
            ORDER BY l.createdAt DESC
            """)
    Page<ActivityLog> searchLogs(
            @Param("action") String action,
            @Param("entityType") String entityType,
            Pageable pageable);
}
