package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.common.enums.BusStatus;
import com.tomzxy.busozy.entity.Bus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusRepository extends JpaRepository<Bus, Long> {

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByCompanyIdAndIsActiveTrue(Long companyId); // used by Phase 3 company delete guard

    @Query("""
            SELECT b FROM Bus b
            WHERE (:companyId IS NULL OR b.company.id = :companyId)
              AND (:status IS NULL OR b.status = :status)
              AND (:keyword IS NULL OR LOWER(b.licensePlate) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(b.busNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Bus> searchBuses(
            @Param("companyId") Long companyId,
            @Param("status") BusStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    long countByCompanyIdAndStatus(Long companyId, BusStatus status);

    Optional<Bus> findByIdAndCompanyId(Long id, Long companyId);
}
