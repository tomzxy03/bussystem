package com.tomzxy.busozy.service.interfaces;

import com.tomzxy.busozy.common.enums.UserStatus;
import com.tomzxy.busozy.dto.request.CompanyReqDTO;
import com.tomzxy.busozy.dto.request.DriverReqDTO;
import com.tomzxy.busozy.dto.response.CompanyResDTO;
import com.tomzxy.busozy.dto.response.DriverResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CompanyDriverService {

    // Company – public
    Page<CompanyResDTO> getCompanies(String keyword, Pageable pageable);

    CompanyResDTO getCompanyById(Long id);

    // Company – admin
    CompanyResDTO createCompany(CompanyReqDTO req);

    CompanyResDTO updateCompany(Long id, CompanyReqDTO req);

    void deleteCompany(Long id);

    // Driver – admin
    List<DriverResDTO> getDriversByCompany(Long companyId);

    DriverResDTO createDriver(DriverReqDTO req);

    DriverResDTO updateDriver(Long id, DriverReqDTO req);

    DriverResDTO updateDriverStatus(Long id, UserStatus status);
}
