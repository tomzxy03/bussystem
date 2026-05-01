package com.tomzxy.busozy.service.impl.vendor;

import com.tomzxy.busozy.common.enums.TripStatus;
import com.tomzxy.busozy.dto.request.TripCreateReqDTO;
import com.tomzxy.busozy.dto.response.TripResDTO;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.repository.BusRepository;
import com.tomzxy.busozy.repository.DriverRepository;
import com.tomzxy.busozy.repository.TripRepository;
import com.tomzxy.busozy.mapper.TripMapper;
import com.tomzxy.busozy.service.impl.AccessScopeService;
import com.tomzxy.busozy.service.interfaces.admin.AdminTripService;
import com.tomzxy.busozy.service.interfaces.vendor.VendorTripService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class VendorTripServiceImpl implements VendorTripService {

    private final BusRepository busRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final TripMapper tripMapper;
    private final AdminTripService adminTripService;
    private final AccessScopeService accessScopeService;

    @Override
    public Page<TripResDTO> searchTrips(User currentUser, TripStatus status, LocalDate departureDate,
            Pageable pageable) {
        return tripRepository
                .searchByCompany(accessScopeService.requireVendorCompanyId(currentUser), status, departureDate,
                        pageable)
                .map(tripMapper::toTripRes);
    }

    @Override
    @Transactional
    public TripResDTO createTrip(User currentUser, TripCreateReqDTO req) {
        assertTripInputOwnedByVendor(currentUser, req);
        return adminTripService.createTrip(req);
    }

    @Override
    @Transactional
    public TripResDTO updateTrip(User currentUser, Long tripId, TripCreateReqDTO req) {
        accessScopeService.assertTripBelongsToVendor(currentUser, tripId);
        assertTripInputOwnedByVendor(currentUser, req);
        return adminTripService.updateTrip(tripId, req);
    }

    @Override
    @Transactional
    public TripResDTO updateTripStatus(User currentUser, Long tripId, TripStatus status) {
        accessScopeService.assertTripBelongsToVendor(currentUser, tripId);
        return adminTripService.updateTripStatus(tripId, status);
    }

    @Override
    @Transactional
    public void deleteTrip(User currentUser, Long tripId) {
        accessScopeService.assertTripBelongsToVendor(currentUser, tripId);
        adminTripService.deleteTrip(tripId);
    }

    private void assertTripInputOwnedByVendor(User currentUser, TripCreateReqDTO req) {
        Long companyId = accessScopeService.requireVendorCompanyId(currentUser);
        if (busRepository.findByIdAndCompanyId(req.getBusId(), companyId).isEmpty()) {
            throw new AccessDeniedException("Bus does not belong to vendor company");
        }
        if (!tripRepository.routeBelongsToCompany(req.getRouteId(), companyId)) {
            throw new AccessDeniedException("Route does not belong to vendor company");
        }
        if (req.getDriverId() != null
                && driverRepository.findByIdAndCompanyId(req.getDriverId(), companyId).isEmpty()) {
            throw new AccessDeniedException("Driver does not belong to vendor company");
        }
    }
}
