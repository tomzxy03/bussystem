package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    boolean existsByCode(String code);

    /**
     * Search routes by keyword, with optional company filter.
     */
    @Query("""
            SELECT r FROM Route r
            WHERE r.isActive = true
              AND (:keyword IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(r.code) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:companyId IS NULL OR r.company.id = :companyId)
            ORDER BY r.name
            """)
    Page<Route> searchRoutes(
            @Param("keyword") String keyword,
            @Param("companyId") Long companyId,
            Pageable pageable);

    /**
     * Find routes connecting origin and destination stops (in the right direction).
     * A route qualifies if: origin stop appears with lower stop_order than
     * destination stop.
     */
    @Query("""
            SELECT DISTINCT r FROM Route r
            JOIN r.stops origin ON origin.stop.id = :originStopId AND origin.isPickup = true
            JOIN r.stops dest ON dest.stop.id = :destStopId AND dest.isDropoff = true
            WHERE r.isActive = true
              AND origin.stopOrder < dest.stopOrder
            """)
    Page<Route> findByOriginAndDestination(
            @Param("originStopId") Long originStopId,
            @Param("destStopId") Long destStopId,
            Pageable pageable);
}
