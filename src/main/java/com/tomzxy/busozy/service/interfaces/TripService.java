package com.tomzxy.busozy.service.interfaces;

import com.tomzxy.busozy.common.enums.TripStatus;
import com.tomzxy.busozy.dto.request.TripCreateReqDTO;
import com.tomzxy.busozy.dto.response.SeatAvailabilityDTO;
import com.tomzxy.busozy.dto.response.TripDetailResDTO;
import com.tomzxy.busozy.dto.response.TripResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface TripService {

    // Public
    Page<TripResDTO> searchTrips(Long originStopId, Long destStopId, LocalDate date, Integer passengers,
            Pageable pageable);

    TripDetailResDTO getTripDetail(Long id);

    List<SeatAvailabilityDTO> getTripSeats(Long tripId, Integer pickupOrder, Integer dropoffOrder);

}
