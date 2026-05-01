package com.tomzxy.busozy.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record BookingDetailResDTO(
        BookingResDTO booking,
        List<BookingSeatResDTO> seats) {
    public record BookingSeatResDTO(
            String seatNumber,
            String seatType,
            BigDecimal finalPrice,
            PassengerResDTO passenger) {
    }

    public record PassengerResDTO(
            String fullName,
            String phone) {
    }
}
