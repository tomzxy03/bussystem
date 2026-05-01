package com.tomzxy.busozy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Config table — no soft-delete, no BaseEntity to keep it lighter.
 * Seeded via V10 Flyway: COD, BANK_TRANSFER (active), MOMO, VNPAY (inactive).
 */
@Entity
@Table(name = "payment_methods")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "fee_percentage", precision = 4, scale = 2)
    private BigDecimal feePercentage = BigDecimal.ZERO;

    @CreatedDate
    private OffsetDateTime createdAt;

    @LastModifiedDate
    private OffsetDateTime updatedAt;
}
