package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.BusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusTypeRepository extends JpaRepository<BusType, Long> {

    boolean existsByCode(String code);

    List<BusType> findByIsActiveTrueOrderByNameAsc();

    boolean existsByIdAndIsActiveTrue(Long id);
}
