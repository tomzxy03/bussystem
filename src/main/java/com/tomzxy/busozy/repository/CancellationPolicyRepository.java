package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.CancellationPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, Long> {

    @EntityGraph(attributePaths = {"company", "route"})
    @Query("""
            SELECT p FROM CancellationPolicy p
            WHERE p.isActive = true
              AND p.company.id = :companyId
              AND p.route.id = :routeId
            ORDER BY p.hoursBeforeDeparture DESC
            """)
    List<CancellationPolicy> findActiveCompanyRoutePolicies(
            @Param("companyId") Long companyId,
            @Param("routeId") Long routeId);

    @EntityGraph(attributePaths = {"company", "route"})
    @Query("""
            SELECT p FROM CancellationPolicy p
            WHERE p.isActive = true
              AND p.company.id = :companyId
              AND p.route IS NULL
            ORDER BY p.hoursBeforeDeparture DESC
            """)
    List<CancellationPolicy> findActiveCompanyPolicies(@Param("companyId") Long companyId);

    @EntityGraph(attributePaths = {"company", "route"})
    @Query("""
            SELECT p FROM CancellationPolicy p
            WHERE p.isActive = true
              AND p.company IS NULL
              AND p.route.id = :routeId
            ORDER BY p.hoursBeforeDeparture DESC
            """)
    List<CancellationPolicy> findActiveRoutePolicies(@Param("routeId") Long routeId);

    @EntityGraph(attributePaths = {"company", "route"})
    @Query("""
            SELECT p FROM CancellationPolicy p
            WHERE p.isActive = true
              AND p.company IS NULL
              AND p.route IS NULL
            ORDER BY p.hoursBeforeDeparture DESC
            """)
    List<CancellationPolicy> findActiveGlobalPolicies();

    @EntityGraph(attributePaths = {"company", "route"})
    @Query("""
            SELECT p FROM CancellationPolicy p
            LEFT JOIN p.company company
            LEFT JOIN p.route route
            WHERE (:companyId IS NULL OR company.id = :companyId)
              AND (:routeId IS NULL OR route.id = :routeId)
              AND (:active IS NULL OR p.isActive = :active)
            ORDER BY p.createdAt DESC
            """)
    Page<CancellationPolicy> searchPolicies(
            @Param("companyId") Long companyId,
            @Param("routeId") Long routeId,
            @Param("active") Boolean active,
            Pageable pageable);
}
