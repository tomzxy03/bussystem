package com.tomzxy.busozy.dto.response;

import com.tomzxy.busozy.entity.Booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.UUID;

public record BookingResDTO(
        UUID bookingCode,
        Long tripId,
        String routeName,
        String companyName,
        LocalDate departureDate,
        OffsetTime departureTime,
        Integer pickupOrder,
        Integer dropoffOrder,
        BigDecimal totalAmount,
        com.tomzxy.busozy.common.enums.BookingStatus status,
        com.tomzxy.busozy.common.enums.BookingPaymentStatus paymentStatus,
        OffsetDateTime expiredAt) {
}
