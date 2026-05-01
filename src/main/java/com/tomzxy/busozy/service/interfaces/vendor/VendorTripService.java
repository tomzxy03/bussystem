package com.tomzxy.busozy.service.interfaces.vendor;

import com.tomzxy.busozy.common.enums.TripStatus;
import com.tomzxy.busozy.dto.request.TripCreateReqDTO;
import com.tomzxy.busozy.dto.response.TripResDTO;
import com.tomzxy.busozy.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface VendorTripService {

    Page<TripResDTO> searchTrips(User currentUser, TripStatus status, LocalDate departureDate, Pageable pageable);

    TripResDTO createTrip(User currentUser, TripCreateReqDTO req);

    TripResDTO updateTrip(User currentUser, Long tripId, TripCreateReqDTO req);

    TripResDTO updateTripStatus(User currentUser, Long tripId, TripStatus status);

    void deleteTrip(User currentUser, Long tripId);
}
