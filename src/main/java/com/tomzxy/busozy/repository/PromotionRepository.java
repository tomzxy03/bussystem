package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

  /**
   * Case-insensitive code lookup for active promotions.
   * Uses LOWER(code) index from V11 migration.
   */
  @Query("SELECT p FROM Promotion p WHERE LOWER(p.code) = LOWER(:code) AND p.isActive = true")
  Optional<Promotion> findByCodeActiveIgnoreCase(@Param("code") String code);

  Page<Promotion> findAllByOrderByCreatedAtDesc(Pageable pageable);

  /**
   * Atomic increment of used_count with optimistic limit check.
   * Returns 1 if updated, 0 if usage_limit already reached.
   */
  @Modifying
  @Query("""
      UPDATE Promotion p SET p.usedCount = p.usedCount + 1
      WHERE p.id = :id
        AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit)
      """)
  int incrementUsedCount(@Param("id") Long id);

  /**
   * Find active promotions applicable to a specific route (for caching by route).
   */
  @Query("""
      SELECT p FROM Promotion p
      LEFT JOIN p.applicableRoutes r
      WHERE p.isActive = true
        AND p.deletedAt IS NULL
        AND (r.id = :routeId OR p.applicableRoutes IS EMPTY)
      """)
  List<Promotion> findActiveByRouteId(@Param("routeId") Long routeId);

  /**
   * Efficiently checks if a promotion allows a specific route without loading the
   * entire Set<Route>.
   * Assumes applicableRoutes is mapped via the ManyToMany relationship.
   */
  @Query("""
      SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
      FROM Promotion p JOIN p.applicableRoutes r
      WHERE p.id = :promoId AND r.id = :routeId
      """)
  boolean existsByPromotionIdAndRouteId(@Param("promoId") Long promoId, @Param("routeId") Long routeId);
}
