package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.Booking;
import com.tomzxy.busozy.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    interface DashboardStatsProjection {
        Long getTotalBookingsToday();

        java.math.BigDecimal getTotalRevenueToday();

        Long getPendingBookingsCount();
    }

    interface RouteStatsProjection {
        String getRouteName();

        Long getTotalBookings();

        java.math.BigDecimal getTotalRevenue();
    }

    Optional<Booking> findByBookingCode(UUID bookingCode);

    Page<Booking> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.user
            WHERE b.trip.id = :tripId
              AND b.status = :status
            """)
    List<Booking> findByTripIdAndStatus(
            @Param("tripId") Long tripId,
            @Param("status") com.tomzxy.busozy.common.enums.BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE (:status IS NULL OR b.status = :status) ORDER BY b.createdAt DESC")
    Page<Booking> findAllForAdmin(@Param("status") com.tomzxy.busozy.common.enums.BookingStatus status, Pageable pageable);

    /**
     * Anti-overbooking overlap check — source of truth.
     * Returns seat IDs that are already booked for overlapping segments.
     *
     * Overlap condition (Qwen-reviewed):
     * NOT (b.dropoff_order <= :pickupOrder OR b.pickup_order >= :dropoffOrder)
     * = booking b overlaps with [pickupOrder, dropoffOrder) segment.
     */
    @Query(value = """
            SELECT bs.seat_id FROM booking_seats bs
            JOIN bookings b ON b.id = bs.booking_id
            WHERE b.trip_id = :tripId
              AND bs.seat_id IN (:seatIds)
              AND b.status NOT IN ('CANCELLED', 'EXPIRED', 'COMPLETED')
              AND b.deleted_at IS NULL
              AND bs.deleted_at IS NULL
              AND NOT (b.dropoff_order <= :pickupOrder OR b.pickup_order >= :dropoffOrder)
            """, nativeQuery = true)
    List<Long> findOverlappingSeats(
            @Param("tripId") Long tripId,
            @Param("seatIds") List<Long> seatIds,
            @Param("pickupOrder") Integer pickupOrder,
            @Param("dropoffOrder") Integer dropoffOrder);

    /**
     * Batch update PENDING bookings whose hold has expired.
     * Used by BookingExpiryScheduler to avoid N+1 updates.
     */
    @Modifying
    @Query("""
            UPDATE Booking b SET b.status = 'EXPIRED'
            WHERE b.status = 'PENDING'
              AND b.reservedUntil < :now
            """)
    int batchExpirePending(@Param("now") OffsetDateTime now);

    @Modifying
    @Query("""
            UPDATE Booking b
            SET b.status = 'COMPLETED'
            WHERE b.trip.id = :tripId
              AND b.status = 'CONFIRMED'
            """)
    int batchMarkCompletedByTripId(@Param("tripId") Long tripId);

    /**
     * Returns tripIds of bookings that were just expired (for cache invalidation).
     * Runs immediately after batchExpirePending in the same scheduler tick.
     */
    @Query("""
            SELECT DISTINCT b.trip.id FROM Booking b
            WHERE b.status = 'EXPIRED'
              AND b.reservedUntil BETWEEN :from AND :to
            """)
    List<Long> findTripIdsOfRecentlyExpired(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    // ─── Vendor-scoped queries ─────────────────────────────────────────────

    /**
     * Vendor list: all bookings for their company's trips, optional status filter.
     */
    @Query("""
            SELECT b FROM Booking b
            JOIN b.trip t
            JOIN t.route r
            WHERE r.company.id = :companyId
              AND (:status IS NULL OR b.status = :status)
            ORDER BY b.createdAt DESC
            """)
    Page<Booking> findByCompanyId(
            @Param("companyId") Long companyId,
            @Param("status") com.tomzxy.busozy.common.enums.BookingStatus status,
            Pageable pageable);

    /**
     * Vendor dashboard: count of PENDING bookings (seat holds awaiting payment).
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            JOIN b.trip t
            JOIN t.route r
            WHERE r.company.id = :companyId
              AND b.status = 'PENDING'
            """)
    long countPendingByCompanyId(@Param("companyId") Long companyId);

    /** Vendor dashboard: today's confirmed revenue (PAID bookings only). */
    @Query("""
            SELECT COALESCE(SUM(b.finalPrice), 0) FROM Booking b
            JOIN b.trip t
            JOIN t.route r
            WHERE r.company.id = :companyId
              AND b.paymentStatus = 'PAID'
              AND CAST(b.createdAt AS LocalDate) = :today
            """)
    java.math.BigDecimal sumTodayRevenueByCompanyId(
            @Param("companyId") Long companyId,
            @Param("today") java.time.LocalDate today);

    @Query("""
            SELECT COUNT(b) AS totalBookingsToday,
                   COALESCE(SUM(CASE
                       WHEN b.paymentStatus = 'PAID' AND CAST(b.createdAt AS LocalDate) = :today
                       THEN b.finalPrice
                       ELSE 0
                   END), 0) AS totalRevenueToday,
                   (SELECT COUNT(pb) FROM Booking pb WHERE pb.status = 'PENDING') AS pendingBookingsCount
            FROM Booking b
            WHERE b.status <> 'EXPIRED'
              AND CAST(b.createdAt AS LocalDate) = :today
            """)
    DashboardStatsProjection getTodayStats(@Param("today") LocalDate today);

    @Query("""
            SELECT r.name AS routeName,
                   COUNT(b) AS totalBookings,
                   COALESCE(SUM(CASE WHEN b.paymentStatus = 'PAID' THEN b.finalPrice ELSE 0 END), 0) AS totalRevenue
            FROM Booking b
            JOIN b.trip t
            JOIN t.route r
            WHERE b.status <> 'EXPIRED'
              AND CAST(b.createdAt AS LocalDate) = :today
            GROUP BY r.name
            ORDER BY COUNT(b) DESC, COALESCE(SUM(CASE WHEN b.paymentStatus = 'PAID' THEN b.finalPrice ELSE 0 END), 0) DESC
            """)
    List<RouteStatsProjection> findTopRouteStats(@Param("today") LocalDate today, Pageable pageable);

    @EntityGraph(attributePaths = {"trip", "trip.route", "trip.route.company"})
    @Query("""
            SELECT b FROM Booking b
            WHERE b.status <> 'EXPIRED'
            ORDER BY b.createdAt DESC
            """)
    List<Booking> findRecentBookings(Pageable pageable);
}
