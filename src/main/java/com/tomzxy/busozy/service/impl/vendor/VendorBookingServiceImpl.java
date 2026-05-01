package com.tomzxy.busozy.service.impl.vendor;

import com.tomzxy.busozy.common.enums.BookingStatus;
import com.tomzxy.busozy.dto.response.BookingResDTO;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.mapper.BookingMapper;
import com.tomzxy.busozy.repository.BookingRepository;
import com.tomzxy.busozy.service.impl.AccessScopeService;
import com.tomzxy.busozy.service.interfaces.vendor.VendorBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VendorBookingServiceImpl implements VendorBookingService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final AccessScopeService accessScopeService;

    @Override
    public Page<BookingResDTO> getVendorBookings(User currentUser, BookingStatus status, Pageable pageable) {
        Long companyId = accessScopeService.requireVendorCompanyId(currentUser);
        return bookingRepository.findByCompanyId(companyId, status, pageable)
                .map(bookingMapper::toBookingRes);
    }
}
