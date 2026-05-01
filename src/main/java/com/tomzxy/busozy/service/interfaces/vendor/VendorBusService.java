package com.tomzxy.busozy.service.interfaces.vendor;

import com.tomzxy.busozy.common.enums.BusStatus;
import com.tomzxy.busozy.dto.request.BusReqDTO;
import com.tomzxy.busozy.dto.request.SeatReqDTO;
import com.tomzxy.busozy.dto.response.BusResDTO;
import com.tomzxy.busozy.dto.response.SeatResDTO;
import com.tomzxy.busozy.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VendorBusService {

    Page<BusResDTO> searchBuses(User currentUser, BusStatus status, String keyword, Pageable pageable);

    BusResDTO createBus(User currentUser, BusReqDTO req);

    BusResDTO updateBus(User currentUser, Long busId, BusReqDTO req);

    BusResDTO updateBusStatus(User currentUser, Long busId, BusStatus status);

    List<SeatResDTO> initSeats(User currentUser, Long busId, List<SeatReqDTO> seats);
}
