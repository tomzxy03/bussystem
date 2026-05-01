package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Long> {

    @Query("""
            SELECT p FROM Province p
            WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY p.name
            """)
    List<Province> searchByKeyword(@Param("keyword") String keyword);
}
