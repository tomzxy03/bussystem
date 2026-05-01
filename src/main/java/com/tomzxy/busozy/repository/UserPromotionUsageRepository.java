package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.UserPromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface UserPromotionUsageRepository extends JpaRepository<UserPromotionUsage, Long> {

    Optional<UserPromotionUsage> findByUserIdAndPromotionId(Long userId, Long promotionId);

    int countByUserIdAndPromotionId(Long userId, Long promotionId);

    /**
     * Upsert user usage: increment existing or create new row.
     * Uses DB UNIQUE(user_id, promotion_id) for conflict resolution.
     */
    @Modifying
    @Query(value = """
            INSERT INTO user_promotion_usage (user_id, promotion_id, used_count, last_used_at)
            VALUES (:userId, :promotionId, 1, :now)
            ON CONFLICT (user_id, promotion_id)
            DO UPDATE SET used_count = user_promotion_usage.used_count + 1,
                          last_used_at = :now
            """, nativeQuery = true)
    void upsertUsage(@Param("userId") Long userId,
            @Param("promotionId") Long promotionId,
            @Param("now") OffsetDateTime now);
}
