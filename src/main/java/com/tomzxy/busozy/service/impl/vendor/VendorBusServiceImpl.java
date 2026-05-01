package com.tomzxy.busozy.service.impl.vendor;

import com.tomzxy.busozy.common.enums.BusStatus;
import com.tomzxy.busozy.dto.request.BusReqDTO;
import com.tomzxy.busozy.dto.request.SeatReqDTO;
import com.tomzxy.busozy.dto.response.BusResDTO;
import com.tomzxy.busozy.dto.response.SeatResDTO;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.service.impl.AccessScopeService;
import com.tomzxy.busozy.service.interfaces.BusService;
import com.tomzxy.busozy.service.interfaces.vendor.VendorBusService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorBusServiceImpl implements VendorBusService {

    private final BusService busService;
    private final AccessScopeService accessScopeService;

    @Override
    public Page<BusResDTO> searchBuses(User currentUser, BusStatus status, String keyword, Pageable pageable) {
        return busService.searchBuses(accessScopeService.requireVendorCompanyId(currentUser), status, keyword,
                pageable);
    }

    @Override
    @Transactional
    public BusResDTO createBus(User currentUser, BusReqDTO req) {
        req.setCompanyId(accessScopeService.requireVendorCompanyId(currentUser));
        return busService.createBus(req);
    }

    @Override
    @Transactional
    public BusResDTO updateBus(User currentUser, Long busId, BusReqDTO req) {
        accessScopeService.assertBusBelongsToVendor(currentUser, busId);
        req.setCompanyId(accessScopeService.requireVendorCompanyId(currentUser));
        return busService.updateBus(busId, req);
    }

    @Override
    @Transactional
    public BusResDTO updateBusStatus(User currentUser, Long busId, BusStatus status) {
        accessScopeService.assertBusBelongsToVendor(currentUser, busId);
        return busService.updateBusStatus(busId, status);
    }

    @Override
    @Transactional
    public List<SeatResDTO> initSeats(User currentUser, Long busId, List<SeatReqDTO> seats) {
        accessScopeService.assertBusBelongsToVendor(currentUser, busId);
        return busService.initOrReplaceSeats(busId, seats);
    }
}
