package com.tomzxy.busozy.entity;

import com.tomzxy.busozy.common.BaseEntity;
import com.tomzxy.busozy.common.enums.BusStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "buses")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class Bus extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_type_id", nullable = false)
    private BusType busType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_layout_id")
    private SeatLayout seatLayout;

    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;

    @Column(name = "bus_number", length = 50)
    private String busNumber;

    @Column(length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BusStatus status = BusStatus.ACTIVE;

    @OneToMany(mappedBy = "bus", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("rowNum ASC, colNum ASC")
    private List<Seat> seats = new ArrayList<>();
}
