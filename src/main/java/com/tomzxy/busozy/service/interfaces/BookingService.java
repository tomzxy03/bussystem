package com.tomzxy.busozy.service.interfaces;

import com.tomzxy.busozy.dto.request.CreateBookingReqDTO;
import com.tomzxy.busozy.dto.response.BookingDetailResDTO;
import com.tomzxy.busozy.dto.response.BookingResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BookingService {

    /**
     * POST /bookings — main booking creation with distributed lock + idempotency
     */
    BookingResDTO createBooking(Long userId, CreateBookingReqDTO req, String idempotencyKey);

    /** GET /bookings/my — paginated user history */
    Page<BookingResDTO> getMyBookings(Long userId, Pageable pageable);

    /** GET /bookings/{code} — detail view authorized to owner */
    BookingDetailResDTO getBookingDetail(Long userId, UUID bookingCode);

    /** POST /bookings/{code}/cancel */
    BookingResDTO cancelBooking(Long userId, UUID bookingCode);

    /**
     * Called internally by PaymentService after a PAID gateway callback.
     * Sets booking.status = CONFIRMED, booking.paymentStatus = PAID, evicts seat
     * cache.
     */
    void confirmPayment(Long bookingId);
}
