package com.tomzxy.busozy.entity;

import com.tomzxy.busozy.common.BaseEntity;
import com.tomzxy.busozy.common.enums.SeatType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "seats")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class Seat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", length = 20)
    private SeatType seatType = SeatType.STANDARD;

    @Column(name = "row_num")
    private Integer rowNum;

    @Column(name = "col_num")
    private Integer colNum;

    @Column(name = "price_multiplier", precision = 4, scale = 2)
    private BigDecimal priceMultiplier = BigDecimal.ONE;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
