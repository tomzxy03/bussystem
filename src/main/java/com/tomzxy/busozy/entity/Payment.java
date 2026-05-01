package com.tomzxy.busozy.entity;

import com.tomzxy.busozy.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "payments")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

    public enum PaymentStatus {
        PENDING, PAID, FAILED, REFUNDED, CANCELLED
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    /** Transaction ID from gateway (MoMo/VNPAY). Null for COD. UNIQUE in DB. */
    @Column(name = "gateway_transaction_id", unique = true, length = 100)
    private String gatewayTransactionId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.tomzxy.busozy.common.enums.PaymentTransactionStatus status = com.tomzxy.busozy.common.enums.PaymentTransactionStatus.PENDING;

    /** Raw webhook payload stored for audit trail and debugging. */
    @Type(JsonBinaryType.class)
    @Column(name = "gateway_response", columnDefinition = "jsonb")
    private Map<String, Object> gatewayResponse;
}
