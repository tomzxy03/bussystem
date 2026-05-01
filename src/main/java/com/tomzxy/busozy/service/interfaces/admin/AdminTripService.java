package com.tomzxy.busozy.service.interfaces.admin;

import com.tomzxy.busozy.common.enums.TripStatus;
import com.tomzxy.busozy.dto.request.TripCreateReqDTO;
import com.tomzxy.busozy.dto.response.TripResDTO;

public interface AdminTripService {
    TripResDTO createTrip(TripCreateReqDTO req);

    TripResDTO updateTrip(Long id, TripCreateReqDTO req);

    TripResDTO updateTripStatus(Long id, TripStatus status);

    void deleteTrip(Long id);
}
