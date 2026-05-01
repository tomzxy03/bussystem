package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {

    List<District> findByProvinceIdOrderByNameAsc(Long provinceId);

    boolean existsByProvinceIdAndId(Long provinceId, Long districtId);
}
