package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.common.enums.RefundStatus;
import com.tomzxy.busozy.entity.Cancellation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CancellationRepository extends JpaRepository<Cancellation, Long> {

    @Override
    @EntityGraph(attributePaths = {"booking", "cancelledBy"})
    Optional<Cancellation> findById(Long id);

    @EntityGraph(attributePaths = {"booking", "cancelledBy"})
    Optional<Cancellation> findByBookingBookingCode(UUID bookingCode);

    @EntityGraph(attributePaths = {"booking", "cancelledBy"})
    Optional<Cancellation> findByBookingId(Long bookingId);

    @EntityGraph(attributePaths = {"booking", "cancelledBy"})
    @Query("""
            SELECT c FROM Cancellation c
            WHERE (:refundStatus IS NULL OR c.refundStatus = :refundStatus)
              AND (:bookingCode IS NULL OR c.booking.bookingCode = :bookingCode)
            ORDER BY c.cancelTime DESC
            """)
    Page<Cancellation> searchAdmin(
            @Param("refundStatus") RefundStatus refundStatus,
            @Param("bookingCode") UUID bookingCode,
            Pageable pageable);
}
