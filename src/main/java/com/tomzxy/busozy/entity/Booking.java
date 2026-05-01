package com.tomzxy.busozy.entity;

import com.tomzxy.busozy.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.tomzxy.busozy.common.enums.BookingStatus;
import com.tomzxy.busozy.common.enums.BookingPaymentStatus;

@Entity
@Table(name = "bookings")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class Booking extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private BookingPaymentStatus paymentStatus = BookingPaymentStatus.PENDING;

    @Column(name = "booking_code", nullable = false, unique = true)
    private UUID bookingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "pickup_order", nullable = false)
    private Integer pickupOrder;

    @Column(name = "dropoff_order", nullable = false)
    private Integer dropoffOrder;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "final_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalPrice;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "reserved_until")
    private OffsetDateTime reservedUntil;

    /** Nullable — set when user applies a promotion code during booking */
    @Column(name = "promotion_id")
    private Long promotionId;

    /** Amount discounted by promotion; 0 when no promotion applied */
    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** Optimistic lock for concurrent status transitions. */
    @Version
    private Long version;

    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeat> seats = new ArrayList<>();
}
