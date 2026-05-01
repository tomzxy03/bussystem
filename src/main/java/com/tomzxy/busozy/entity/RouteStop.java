package com.tomzxy.busozy.entity;

import com.tomzxy.busozy.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "route_stops")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class RouteStop extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", nullable = false)
    private Stop stop;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Column(name = "estimated_minutes_from_origin", nullable = false)
    private Integer estimatedMinutesFromOrigin;

    @Column(name = "distance_from_origin", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceFromOrigin;

    @Column(name = "is_pickup")
    private Boolean isPickup = true;

    @Column(name = "is_dropoff")
    private Boolean isDropoff = true;
}
