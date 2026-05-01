package com.tomzxy.busozy.dto.response;

import com.tomzxy.busozy.common.enums.TripStatus;

import java.time.LocalDate;
import java.time.OffsetTime;

public record TripResDTO(
        Long id,
        String routeCode,
        String routeName,
        String companyName,
        String busLicensePlate,
        String busTypeName,
        String driverName,
        LocalDate departureDate,
        OffsetTime departureTime,
        TripStatus status) {
}
