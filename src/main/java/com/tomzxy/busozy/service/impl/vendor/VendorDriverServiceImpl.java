package com.tomzxy.busozy.service.impl.vendor;

import com.tomzxy.busozy.common.enums.UserStatus;
import com.tomzxy.busozy.dto.request.DriverReqDTO;
import com.tomzxy.busozy.dto.response.DriverResDTO;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.service.impl.AccessScopeService;
import com.tomzxy.busozy.service.interfaces.CompanyDriverService;
import com.tomzxy.busozy.service.interfaces.vendor.VendorDriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorDriverServiceImpl implements VendorDriverService {

    private final CompanyDriverService companyDriverService;
    private final AccessScopeService accessScopeService;

    @Override
    public List<DriverResDTO> getDrivers(User currentUser) {
        return companyDriverService.getDriversByCompany(accessScopeService.requireVendorCompanyId(currentUser));
    }

    @Override
    @Transactional
    public DriverResDTO createDriver(User currentUser, DriverReqDTO req) {
        req.setCompanyId(accessScopeService.requireVendorCompanyId(currentUser));
        return companyDriverService.createDriver(req);
    }

    @Override
    @Transactional
    public DriverResDTO updateDriver(User currentUser, Long driverId, DriverReqDTO req) {
        accessScopeService.assertDriverBelongsToVendor(currentUser, driverId);
        req.setCompanyId(accessScopeService.requireVendorCompanyId(currentUser));
        return companyDriverService.updateDriver(driverId, req);
    }

    @Override
    @Transactional
    public DriverResDTO updateDriverStatus(User currentUser, Long driverId, UserStatus status) {
        accessScopeService.assertDriverBelongsToVendor(currentUser, driverId);
        return companyDriverService.updateDriverStatus(driverId, status);
    }
}
