package com.tomzxy.busozy.service.interfaces.vendor;

import com.tomzxy.busozy.common.enums.BookingStatus;
import com.tomzxy.busozy.dto.response.BookingResDTO;
import com.tomzxy.busozy.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VendorBookingService {

    Page<BookingResDTO> getVendorBookings(User currentUser, BookingStatus status, Pageable pageable);
}
