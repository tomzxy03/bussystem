package com.tomzxy.busozy.entity;

import com.tomzxy.busozy.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Type;

import java.util.Map;

@Entity
@Table(name = "seat_layouts")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class SeatLayout extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_type_id", nullable = false)
    private BusType busType;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Type(JsonBinaryType.class)
    @Column(name = "layout_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> layoutData;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;
}
