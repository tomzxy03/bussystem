package com.tomzxy.busozy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Tracks how many times each user has used a specific promotion code.
 * Enforces per_user_limit without race conditions via DB UNIQUE(user_id,
 * promotion_id).
 * Not soft-deleted — usage history must be preserved for auditing.
 */
@Entity
@Table(name = "user_promotion_usage")
@Getter
@Setter
@NoArgsConstructor
public class UserPromotionUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(name = "used_count")
    private Integer usedCount = 0;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;
}
