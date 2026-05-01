package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.common.enums.StopType;
import com.tomzxy.busozy.entity.Stop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StopRepository extends JpaRepository<Stop, Long> {

    boolean existsByCode(String code);

    @Query("""
            SELECT s FROM Stop s
            WHERE (:provinceId IS NULL OR s.province.id = :provinceId)
              AND (:districtId IS NULL OR s.district.id = :districtId)
              AND (:type IS NULL OR s.type = :type)
              AND (:isMajor IS NULL OR s.isMajor = :isMajor)
              AND (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.address) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Stop> searchStops(
            @Param("provinceId") Long provinceId,
            @Param("districtId") Long districtId,
            @Param("type") StopType type,
            @Param("isMajor") Boolean isMajor,
            @Param("keyword") String keyword,
            Pageable pageable);
}
