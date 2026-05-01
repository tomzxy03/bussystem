package com.tomzxy.busozy.service.impl;

import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.entity.Bus;
import com.tomzxy.busozy.entity.Driver;
import com.tomzxy.busozy.entity.Route;
import com.tomzxy.busozy.entity.Trip;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.repository.BusRepository;
import com.tomzxy.busozy.repository.DriverRepository;
import com.tomzxy.busozy.repository.RouteRepository;
import com.tomzxy.busozy.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessScopeService {

    private final DriverRepository driverRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;

    public Long requireVendorCompanyId(User user) {
        if (user == null || !user.isVendor() || user.getCompany() == null) {
            throw new AccessDeniedException("Vendor account is not linked to any company");
        }
        return user.getCompany().getId();
    }

    public void assertDriverBelongsToVendor(User user, Long driverId) {
        Long companyId = requireVendorCompanyId(user);
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DRIVER_NOT_FOUND));
        if (!driver.getCompany().getId().equals(companyId)) {
            throw new AccessDeniedException("Driver does not belong to vendor company");
        }
    }

    public void assertBusBelongsToVendor(User user, Long busId) {
        Long companyId = requireVendorCompanyId(user);
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BUS_NOT_FOUND));
        if (!bus.getCompany().getId().equals(companyId)) {
            throw new AccessDeniedException("Bus does not belong to vendor company");
        }
    }

    public void assertRouteBelongsToVendor(User user, Long routeId) {
        Long companyId = requireVendorCompanyId(user);
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ROUTE_NOT_FOUND));
        if (!route.getCompany().getId().equals(companyId)) {
            throw new AccessDeniedException("Route does not belong to vendor company");
        }
    }

    public void assertTripBelongsToVendor(User user, Long tripId) {
        Long companyId = requireVendorCompanyId(user);
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TRIP_NOT_FOUND));
        if (!trip.getRoute().getCompany().getId().equals(companyId)) {
            throw new AccessDeniedException("Trip does not belong to vendor company");
        }
    }
}
