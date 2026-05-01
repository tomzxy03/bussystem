package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.RoutePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoutePriceRepository extends JpaRepository<RoutePrice, Long> {

    List<RoutePrice> findByRouteIdOrderByPickupOrderAscDropoffOrderAsc(Long routeId);

    void deleteAllByRouteId(Long routeId);

    @Query("SELECT rp FROM RoutePrice rp JOIN FETCH rp.route r WHERE r.id = :routeId AND rp.deletedAt IS NULL")
    List<RoutePrice> findByRouteIdWithRoute(@Param("routeId") Long routeId);

    java.util.Optional<RoutePrice> findByRouteIdAndPickupOrderAndDropoffOrder(
            Long routeId, Integer pickupOrder, Integer dropoffOrder);
}
