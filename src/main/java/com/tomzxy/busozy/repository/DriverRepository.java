package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.common.enums.UserStatus;
import com.tomzxy.busozy.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findByCompanyIdOrderByFullNameAsc(Long companyId);

    boolean existsByCompanyIdAndLicenseNumber(Long companyId, String licenseNumber);

    boolean existsByCompanyIdAndPhone(Long companyId, String phone);

    long countByCompanyIdAndStatus(Long companyId, UserStatus status);

    Optional<Driver> findByIdAndCompanyId(Long id, Long companyId);
}
