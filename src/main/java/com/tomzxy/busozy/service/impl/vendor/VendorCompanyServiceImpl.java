package com.tomzxy.busozy.service.impl.vendor;

import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.dto.request.CompanyReqDTO;
import com.tomzxy.busozy.dto.response.CompanyResDTO;
import com.tomzxy.busozy.entity.Company;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.mapper.CompanyDriverMapper;
import com.tomzxy.busozy.repository.CompanyRepository;
import com.tomzxy.busozy.service.impl.AccessScopeService;
import com.tomzxy.busozy.service.interfaces.CompanyDriverService;
import com.tomzxy.busozy.service.interfaces.vendor.VendorCompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VendorCompanyServiceImpl implements VendorCompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyDriverMapper companyDriverMapper;
    private final CompanyDriverService companyDriverService;
    private final AccessScopeService accessScopeService;

    @Override
    public CompanyResDTO getCompany(User currentUser) {
        return companyDriverMapper.toCompanyRes(requireVendorCompany(currentUser));
    }

    @Override
    @Transactional
    public CompanyResDTO updateCompany(User currentUser, CompanyReqDTO req) {
        return companyDriverService.updateCompany(accessScopeService.requireVendorCompanyId(currentUser), req);
    }

    private Company requireVendorCompany(User currentUser) {
        Long companyId = accessScopeService.requireVendorCompanyId(currentUser);
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMPANY_NOT_FOUND));
    }
}
