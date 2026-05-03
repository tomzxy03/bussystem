package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    /**
     * Check if a bus is already assigned to any trip on that date (conflict check).
     */
    @Query("""
            SELECT COUNT(t) > 0 FROM Trip t
            WHERE t.bus.id = :busId
              AND t.departureDate = :date
              AND t.status <> 'CANCELLED'
              AND t.id <> :excludeId
            """)
    boolean existsBusConflict(
            @Param("busId") Long busId,
            @Param("date") LocalDate date,
            @Param("excludeId") Long excludeId);

    /**
     * Search trips by departure date and matching route segments that cover the
     * origin → destination pair (directional stop_order check mirroring
     * RouteRepository).
     */
    @Query("""
            SELECT DISTINCT t FROM Trip t
            JOIN t.route.stops origin
              ON origin.stop.id = :originStopId AND origin.isPickup = true
            JOIN t.route.stops dest
              ON dest.stop.id = :destStopId AND dest.isDropoff = true
            WHERE t.departureDate = :date
              AND origin.stopOrder < dest.stopOrder
              AND t.status NOT IN ('CANCELLED', 'COMPLETED')
            """)
    Page<Trip> searchAvailable(
            @Param("originStopId") Long originStopId,
            @Param("destStopId") Long destStopId,
            @Param("date") LocalDate date,
            Pageable pageable);

    List<Trip> findByRouteIdAndDepartureDateOrderByDepartureTimeAsc(Long routeId, LocalDate date);

    @Query("""
            SELECT DISTINCT t FROM Trip t
            WHERE t.route.company.id = :companyId
              AND (:status IS NULL OR t.status = :status)
              AND (:departureDate IS NULL OR t.departureDate = :departureDate)
            """)
    Page<Trip> searchByCompany(
            @Param("companyId") Long companyId,
            @Param("status") com.tomzxy.busozy.common.enums.TripStatus status,
            @Param("departureDate") LocalDate departureDate,
            Pageable pageable);

    @Query("""
            SELECT COUNT(t) FROM Trip t
            WHERE t.route.company.id = :companyId
              AND t.status IN :statuses
            """)
    long countByRouteCompanyIdAndStatusIn(
            @Param("companyId") Long companyId,
            @Param("statuses") List<com.tomzxy.busozy.common.enums.TripStatus> statuses);

    @Query("""
            SELECT COUNT(r) > 0 FROM Route r
            WHERE r.id = :routeId
              AND r.company.id = :companyId
            """)
    boolean routeBelongsToCompany(@Param("routeId") Long routeId, @Param("companyId") Long companyId);

    @Query("""
            SELECT COUNT(t) FROM Trip t
            WHERE t.status IN :statuses
            """)
    long countByStatusIn(@Param("statuses") List<com.tomzxy.busozy.common.enums.TripStatus> statuses);
}
