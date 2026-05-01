package com.tomzxy.busozy.service.interfaces.admin;

import com.tomzxy.busozy.common.enums.BookingStatus;
import com.tomzxy.busozy.dto.response.BookingResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminBookingService {
    Page<BookingResDTO> getAdminBookings(BookingStatus status, Pageable pageable);
}
