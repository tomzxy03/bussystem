package com.tomzxy.busozy.service.interfaces;

import com.tomzxy.busozy.common.enums.BusStatus;
import com.tomzxy.busozy.dto.request.BusReqDTO;
import com.tomzxy.busozy.dto.request.BusTypeReqDTO;
import com.tomzxy.busozy.dto.request.SeatReqDTO;
import com.tomzxy.busozy.dto.response.BusResDTO;
import com.tomzxy.busozy.dto.response.BusTypeResDTO;
import com.tomzxy.busozy.dto.response.SeatLayoutResDTO;
import com.tomzxy.busozy.dto.response.SeatResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface BusService {

    // Bus Types – public
    List<BusTypeResDTO> getAllBusTypes();

    BusTypeResDTO getBusTypeById(Long id);

    // Bus Types – admin
    BusTypeResDTO createBusType(BusTypeReqDTO req);

    // Seat Layouts – admin
    SeatLayoutResDTO createSeatLayout(Long busTypeId, String name, Map<String, Object> layoutData);

    // Buses – admin
    Page<BusResDTO> searchBuses(Long companyId, BusStatus status, String keyword, Pageable pageable);

    BusResDTO createBus(BusReqDTO req);

    BusResDTO updateBus(Long id, BusReqDTO req);

    BusResDTO updateBusStatus(Long id, BusStatus status);

    // Seats
    List<SeatResDTO> getBusSeats(Long busId);

    List<SeatResDTO> initOrReplaceSeats(Long busId, List<SeatReqDTO> seats);
}
