package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByTaxCode(String taxCode);

    Optional<Company> findByTaxCode(String taxCode);

    @Query("""
            SELECT c FROM Company c
            WHERE c.isActive = true
              AND (:keyword IS NULL
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY c.name
            """)
    Page<Company> searchActiveCompanies(@Param("keyword") String keyword, Pageable pageable);
}
