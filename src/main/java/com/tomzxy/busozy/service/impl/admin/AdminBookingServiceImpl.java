package com.tomzxy.busozy.service.impl.admin;

import com.tomzxy.busozy.common.enums.BookingStatus;
import com.tomzxy.busozy.dto.response.BookingResDTO;
import com.tomzxy.busozy.mapper.BookingMapper;
import com.tomzxy.busozy.repository.BookingRepository;
import com.tomzxy.busozy.service.interfaces.admin.AdminBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminBookingServiceImpl implements AdminBookingService {

    private final BookingRepository bookingRepository;
    private final BookingMapper mapper;

    @Override
    public Page<BookingResDTO> getAdminBookings(BookingStatus status, Pageable pageable) {
        return bookingRepository.findAllForAdmin(status, pageable).map(mapper::toBookingRes);
    }
}
