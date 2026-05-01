package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByBusIdAndIsActiveTrueOrderByRowNumAscColNumAsc(Long busId);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.bus.id = :busId AND s.isActive = true AND s.deletedAt IS NULL")
    long countActiveByBusId(@Param("busId") Long busId);

    boolean existsByBusIdAndSeatNumber(Long busId, String seatNumber);

    List<Seat> findByBusIdAndSeatNumberIn(Long busId, List<String> seatNumbers);
}
